package org.xalgorithms.rules.steps

import org.xalgorithms.rules.{ Context }
import org.xalgorithms.rules.elements.{ Reference, When }

class FilterStep(val table: Reference, val filters: Seq[When]) extends Step {
  def execute(ctx: Context) {
  }
}

