package blarney_compiler

import chisel3._
import chisel3.util._
import chiseltest._

import scala.collection.parallel.CollectionConverters._
import scala.concurrent.{Future, ExecutionContext}

class BlarneyAST {
  class RTLAction(statement: Any)

  sealed trait Recipe
  case class Skip(next: Recipe) extends Recipe
  case class Tick(next: Recipe) extends Recipe
  case class Action(a: RTLAction) extends Recipe
  case class Seq(recipes: List[Recipe]) extends Recipe
  case class Par(recipes: List[Recipe]) extends Recipe
  case class Wait(cond: Boolean) extends Recipe
  case class When(cond: Boolean, expr: Recipe) extends Recipe
  case class If(cond: Boolean, thenCase: Recipe, elseCase: Recipe) extends Recipe
  case class While(cond: Boolean, loop: Recipe) extends Recipe
  case class Background(recipe: Recipe) extends Recipe

  def runRecipe(recipe: Recipe, clk: Clock): Any = recipe match {
    case Skip(next) =>
      next
    case Tick(next) =>
      clk.step()
      next
    case Action(a) => a
    case Seq(recipes) =>
      for (r <- recipes) {
        runRecipe(r, clk)
        clk.step(1)
      }
    case Par(recipes) =>
      val parList = recipes.par
      parList.foreach(runRecipe(_, clk))
    case Wait(cond) =>
      while (!cond) {
        clk.step()
      }
    case When(cond, expr) =>
      if (cond) {
        runRecipe(expr, clk)
      }
    case If(cond, thenCase, elseCase) =>
      if (cond) {
        runRecipe(thenCase, clk)
      } else {
        runRecipe(elseCase, clk)
      }
    case While(cond, loop) =>
      while (!cond) {
        runRecipe(loop, clk)
      }
    case Background(recipe) => Future {
      runRecipe(recipe, clk)
    }(ExecutionContext.global)
  }
}
