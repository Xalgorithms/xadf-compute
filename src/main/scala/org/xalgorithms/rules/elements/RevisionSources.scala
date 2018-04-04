package org.xalgorithms.rules.elements

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
