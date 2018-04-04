package org.xalgorithms.rules

import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.io.Source

class Step {
}

class PackagedTableReference(val package_name: String, val id: String, val version: String, val name: String) {
}

class Column(val table: Reference, val sources: Seq[TableSource]) {
}

class TableSource(val whens: Seq[When]) {
}

class ColumnTableSource(val name: String, val source: String, whens: Seq[When]) extends TableSource(whens) {
}

class ColumnsTableSource(val columns: Seq[String], whens: Seq[When]) extends TableSource(whens) {
}

class Value {
}

class Reference(val section: String, val key: String) extends Value {
}

class Number(val value: Double) extends Value {
}

class StringValue(val value: String) extends Value {
}

class When(val left: Value, val right: Value, val op: String) {
}

class Assignment(val target: String, val source: Value) {
}

class RevisionSource(val column: String, val whens: Seq[When]) {
}

class TableRevisionSource(
  column: String, whens: Seq[When], val table: Reference) extends RevisionSource(column, whens) {
}

class AddRevisionSource(
  column: String, whens: Seq[When], table: Reference) extends TableRevisionSource(column, whens, table) {
}

class UpdateRevisionSource(
  column: String, whens: Seq[When], table: Reference) extends TableRevisionSource(column, whens, table) {
}

class DeleteRevisionSource(column: String, whens: Seq[When]) extends RevisionSource(column, whens) {
}

class Revision(val source: RevisionSource) {
}

class AssembleStep(val name: String, val columns: Seq[Column]) extends Step {
}

class FilterStep(val table: Reference, val filters: Seq[When]) extends Step {
}

class KeepStep(val name: String, val table: String) extends Step {
}

class AssignmentStep(val table: Reference, val assignments: Seq[Assignment]) extends Step {
}

class MapStep(table: Reference, assignments: Seq[Assignment]) extends AssignmentStep(table, assignments) {
}

class ReduceStep(
  val filters: Seq[When],
  table: Reference, assignments: Seq[Assignment]) extends AssignmentStep(table, assignments) {
}

class RequireStep(val table_reference: PackagedTableReference, val indexes: Seq[String]) extends Step {
}

class ReviseStep(val table: Reference, val revisions: Seq[Revision]) extends Step {
}

object StepProduce {
  implicit val columnReads: Reads[Column] = (
    (JsPath \ "table").read[JsObject] and
    (JsPath \ "sources").read[JsArray]
  )(produce_column _)

  implicit val referenceReads: Reads[Reference] = (
    (JsPath \ "section").read[String] and
    (JsPath \ "key").read[String]
  )(produce_reference _)

  implicit val colsSourceReads: Reads[ColumnsTableSource] = (
    (JsPath \ "columns").read[JsArray] and
    (JsPath \ "whens").read[JsArray]
  )(produce_columns_table_source _)

  implicit val colSourceReads: Reads[ColumnTableSource] = (
    (JsPath \ "name").read[String] and
    (JsPath \ "source").read[String] and
    (JsPath \ "whens").read[JsArray]
  )(produce_column_table_source _)

  implicit val whenReads: Reads[When] = (
    (JsPath \ "left").read[JsObject] and
    (JsPath \ "right").read[JsObject] and
    (JsPath \ "op").read[String]
  )(produce_when _)

  implicit val valueReads: Reads[Value] = (
    (JsPath \ "type").read[String] and
    (JsPath).read[JsObject]
  )(produce_value _)

  implicit val assignmentReads: Reads[Assignment] = (
    (JsPath \ "target").read[String] and
    (JsPath \ "source").read[JsObject]
  )(produce_assignment _)

  implicit val revisionReads : Reads[Revision] = (
    (JsPath \ "op").read[String] and
    (JsPath \ "source").read[JsObject]
  )(produce_revision _)

  implicit val addRevisionSourceReads : Reads[AddRevisionSource] = (
    (JsPath \ "column").read[String] and
    (JsPath \ "table").read[JsObject] and
    (JsPath \ "whens").read[JsArray]
  )(produce_add_revision_source _)
  
  implicit val updateRevisionSourceReads : Reads[UpdateRevisionSource] = (
    (JsPath \ "column").read[String] and
    (JsPath \ "table").read[JsObject] and
    (JsPath \ "whens").read[JsArray]
  )(produce_update_revision_source _)

  implicit val deleteRevisionSourceReads : Reads[DeleteRevisionSource] = (
    (JsPath \ "column").read[String] and
    (JsPath \ "whens").read[JsArray]
  )(produce_delete_revision_source _)

  def stringOrNull(content: JsObject, k: String): String = {
    return (content \ k).validate[String].getOrElse(null)
  }

  def doubleOrNull(content: JsObject, k: String): Double = {
    var rv = null.asInstanceOf[Double]
    val sv = stringOrNull(content, k)

    if (null != sv) {
      rv = sv.toDouble
    }

    return rv
  }

  def produce_packaged_table_reference(content: JsObject): PackagedTableReference = {
    return new PackagedTableReference(
      stringOrNull(content, "package"),
      stringOrNull(content, "id"),
      stringOrNull(content, "version"),
      stringOrNull(content, "name")
    )
  }

  def produce_columns_table_source(
    columns: JsArray, whens: JsArray): ColumnsTableSource = {
    return new ColumnsTableSource(
      columns.validate[Seq[String]].getOrElse(Seq()),
      whens.validate[Seq[When]].getOrElse(Seq())
    )
  }

  def produce_column_table_source(
    name: String, source: String, whens: JsArray): ColumnTableSource = {
    return new ColumnTableSource(
      name, source,
      whens.validate[Seq[When]].getOrElse(Seq())
    )
  }

  def produce_when(left: JsObject, right: JsObject, op: String): When = {
    return new When(
      left.validate[Value].getOrElse(null),
      right.validate[Value].getOrElse(null),
      op)
  }

  def produce_value(vt: String, content: JsObject): Value = {
    if ("string" == vt) {
      return new StringValue(stringOrNull(content, "value"))
    } else if ("number" == vt) {
      return new Number(doubleOrNull(content, "value"))
    } else if ("reference" == vt) {
      return new Reference(stringOrNull(content, "section"), stringOrNull(content, "key"))
    }

    return null
  }

  def produce_assignment(target: String, source: JsObject): Assignment = {
    return new Assignment(
      target, source.validate[Value].getOrElse(null)
    )
  }

  def produce_revision(op: String, source: JsObject): Revision = {
    if (op == "add") {
      return new Revision(
        source.validate[AddRevisionSource].getOrElse(null)
      )
    } else if (op == "update") {
      return new Revision(
        source.validate[UpdateRevisionSource].getOrElse(null)
      )
    } else if (op == "delete") {
      return new Revision(
        source.validate[DeleteRevisionSource].getOrElse(null)
      )
    }

    return null
  }

  def produce_add_revision_source(column: String, table: JsObject, whens: JsArray): AddRevisionSource = {
    return new AddRevisionSource(
      column,
      whens.validate[Seq[When]].getOrElse(Seq()),
      table.validate[Reference].getOrElse(null)
    )
  }

  def produce_update_revision_source(column: String, table: JsObject, whens: JsArray): UpdateRevisionSource = {
    return new UpdateRevisionSource(
      column,
      whens.validate[Seq[When]].getOrElse(Seq()),
      table.validate[Reference].getOrElse(null)
    )
  }

  def produce_delete_revision_source(column: String, whens: JsArray): DeleteRevisionSource = {
    return new DeleteRevisionSource(
      column,
      whens.validate[Seq[When]].getOrElse(Seq())
    )
  }

  def produce_column(table_reference: JsObject, sources: JsArray): Column = {
    return new Column(
      table_reference.validate[Reference].getOrElse(null),
      sources.validate[Seq[ColumnsTableSource]].getOrElse(Seq()) ++
        sources.validate[Seq[ColumnTableSource]].getOrElse(Seq())
    )
  }

  def produce_reference(section: String, key: String): Reference = {
    return new Reference(section, key)
  }

  def produce_assemble(content: JsObject): Step = {
    return new AssembleStep(
      stringOrNull(content, "table_name"),
      (content \ "columns").validate[Seq[Column]].getOrElse(null)
    )
  }

  def produce_filter(content: JsObject): Step = {
    return new FilterStep(
      (content \ "table").validate[Reference].getOrElse(null),
      (content \ "filters").validate[Seq[When]].getOrElse(Seq())
    )
  }

  def produce_keep(content: JsObject): Step = {
    return new KeepStep(stringOrNull(content, "name"), stringOrNull(content, "table_name"))
  }

  def produce_map(content: JsObject): Step = {
    return new MapStep(
      (content \ "table").validate[Reference].getOrElse(null),
      (content \ "assignments").validate[Seq[Assignment]].getOrElse(Seq())
    )
  }

  def produce_reduce(content: JsObject): Step = {
    return new ReduceStep(
      (content \ "filters").validate[Seq[When]].getOrElse(Seq()),
      (content \ "table").validate[Reference].getOrElse(null),
      (content \ "assignments").validate[Seq[Assignment]].getOrElse(Seq())
    )
  }

  def produce_require(content: JsObject): Step = {
    return new RequireStep(
      produce_packaged_table_reference((content \ "reference").as[JsObject]),
      (content \ "indexes").as[Seq[String]])
  }

  def produce_revise(content: JsObject): Step = {
    return new ReviseStep(
      (content \ "table").validate[Reference].getOrElse(null),
      (content \ "revisions").validate[Seq[Revision]].getOrElse(Seq())
    )
  }

  val fns = Map[String, (JsObject) => Step](
    "assemble" -> produce_assemble,
    "filter" -> produce_filter,
    "keep" -> produce_keep,
    "map" -> produce_map,
    "reduce" -> produce_reduce,
    "require" -> produce_require,
    "revise" -> produce_revise
  )

  def apply(name: String, content: JsObject): Step = {
    return fns(name)(content)
  }
}

object SyntaxFromSource {
  implicit val stepReads: Reads[Step] = (
    (JsPath \ "name").read[String] and
    (JsPath).read[JsObject]
  )(StepProduce.apply _)

  def apply(source: Source): Seq[Step] = {
    val res = (Json.parse(source.mkString) \ "steps").validate[Seq[Step]]
    res.getOrElse(Seq())
  }
}