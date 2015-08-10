package com.avaje.ebean.dbmigration.ddlgeneration.platform;

import com.avaje.ebean.dbmigration.ddlgeneration.ColumnDdl;
import com.avaje.ebean.dbmigration.ddlgeneration.DdlBuffer;
import com.avaje.ebean.dbmigration.ddlgeneration.DdlWrite;
import com.avaje.ebean.dbmigration.migration.AddColumn;
import com.avaje.ebean.dbmigration.migration.AlterColumn;
import com.avaje.ebean.dbmigration.migration.Column;
import com.avaje.ebean.dbmigration.migration.DropColumn;

import java.io.IOException;
import java.util.List;

/**
 */
public class BaseColumnDdl implements ColumnDdl {

  protected final PlatformDdl platformDdl;

  public BaseColumnDdl(PlatformDdl platformDdl) {
    this.platformDdl = platformDdl;
  }

  @Override
  public void generate(DdlWrite writer, AddColumn addColumn) throws IOException {

    String tableName = addColumn.getTableName();
    List<Column> columns = addColumn.getColumn();
    for (Column column : columns) {
      // apply
      alterTableAddColumn(writer.apply(), tableName, column);

      // rollback
      alterTableDropColumn(writer.rollback(), tableName, column.getName());
    }
  }

  @Override
  public void generate(DdlWrite writer, DropColumn dropColumn) throws IOException {

    String tableName = dropColumn.getTableName();

    alterTableDropColumn(writer.apply(), tableName, dropColumn.getColumnName());

    // no good rollback option here, it is best if drop columns
    // are put into a separate changeSet that is run last
  }

  @Override
  public void generate(DdlWrite writer, AlterColumn alterColumn) throws IOException {

    if (isTrue(alterColumn.isHistoryExclude())) {
      historyExcludeColumn(writer, alterColumn);
    } else if (isFalse(alterColumn.isHistoryExclude())) {
      historyIncludeColumn(writer, alterColumn);
    }

    if (hasValue(alterColumn.getOldReferences())) {
      dropForeignKey(writer, alterColumn);
    }
    if (hasValue(alterColumn.getNewReferences())) {
      addForeignKey(writer, alterColumn);
    }

    if (isTrue(alterColumn.isUnique())) {
      addUniqueConstraint(writer, alterColumn);
    } else if (isFalse(alterColumn.isUnique())) {
      dropUniqueConstraint(writer, alterColumn);
    }

    if (isTrue(alterColumn.isUniqueOneToOne())) {
      addUniqueOneToOneConstraint(writer, alterColumn);
    } else if (isFalse(alterColumn.isUniqueOneToOne())) {
      dropUniqueOneToOneConstraint(writer, alterColumn);
    }

  }



  protected void addForeignKey(DdlWrite writer, AlterColumn alterColumn) {

  }

  protected void dropForeignKey(DdlWrite writer, AlterColumn alterColumn) {

  }

  protected void dropUniqueOneToOneConstraint(DdlWrite writer, AlterColumn alterColumn) {

  }

  protected void addUniqueOneToOneConstraint(DdlWrite writer, AlterColumn alterColumn) {

  }

  protected void dropUniqueConstraint(DdlWrite writer, AlterColumn alter) throws IOException {

    String tableName = alter.getTableName();
    String columnName = alter.getColumnName();
    String uqName = platformDdl.namingConvention.uniqueConstraintName(tableName, columnName, 50);

    writer.apply()
        .append(platformDdl.dropIndex(uqName, tableName))
        .endOfStatement();
  }

  protected void addUniqueConstraint(DdlWrite writer, AlterColumn alter) throws IOException {

    String tableName = alter.getTableName();
    String columnName = alter.getColumnName();
    String uqName = platformDdl.namingConvention.uniqueConstraintName(tableName, columnName, 50);

    String[] cols = {columnName};
    writer.apply()
        .append(platformDdl.createExternalUniqueForOneToOne(uqName, tableName, cols))
        .endOfStatement();

    writer.rollbackForeignKeys()
        .append(platformDdl.dropIndex(uqName, tableName))
        .endOfStatement();
  }

  protected void historyIncludeColumn(DdlWrite writer, AlterColumn alterColumn) {
    platformDdl.historyIncludeColumn(writer, alterColumn);
  }

  protected void historyExcludeColumn(DdlWrite writer, AlterColumn alterColumn) {
    platformDdl.historyExcludeColumn(writer, alterColumn);
  }

  protected void alterTableDropColumn(DdlBuffer buffer, String tableName, String columnName) throws IOException {

    buffer.append("alter table ").append(tableName)
        .append(" drop column ").append(columnName)
        .endOfStatement().end();
  }

  protected void alterTableAddColumn(DdlBuffer buffer, String tableName, Column column) throws IOException {

    buffer.append("alter table ").append(tableName)
        .append(" add column ").append(column.getName())
        .append(" ").append(column.getType());

    if (Boolean.TRUE.equals(column.isNotnull())) {
      buffer.append(" not null");
    }
    if (hasValue(column.getCheckConstraint())) {
      buffer.append(" ").append(column.getCheckConstraint());
    }
    buffer.endOfStatement().end();
  }


  protected boolean isFalse(Boolean value) {
    return value != null && !value;
  }

  protected boolean isTrue(Boolean value) {
    return value != null && value;
  }

  protected boolean hasValue(String value) {
    return value != null && !value.trim().isEmpty();
  }
}