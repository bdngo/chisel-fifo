package calyx_compiler

class CalyxAST {
  case class CompVar(name: String) {
    def port(port: String): CompPort = CompPort(this, port)
    def addSuffix(suffix: String): CompVar = CompVar(s"$name$suffix")
  }

  case class PortDef(
                      id: CompVar,
                      width: Int,
                      attrs: List[(String, Int)] = List()
                    )

  sealed trait Port {
    def isHole(): Boolean = this match {
      case _: HolePort => true
      case _ => false
    }

    def isConstant(value: Int, width: Int): Boolean = this match {
      case ConstantPort(v, w) if v == value && w == width => true
      case _ => false
    }
  }

  case class CompPort(id: CompVar, name: String) extends Port
  case class ThisPort(id: CompVar) extends Port
  case class HolePort(id: CompVar, name: String) extends Port

  case class ConstantPort(width: Int, value: BigInt) extends Port

  sealed trait Structure
  case class Cell(
                   name: CompVar,
                   comp: CompInst,
                   ref: Boolean,
                   attributes: List[(String, Int)]
                 ) extends Structure

  case class Group(
                    id: CompVar,
                    connections: List[Assign],
                    staticDelay: Option[Int],
                    // True if the group is combinational
                    comb: Boolean
                  ) extends Structure

  case class Assign(src: Port, dest: Port, guard: GuardExpr = True)
    extends Structure

  object Group {
    def fromStructure(
                       id: CompVar,
                       structure: List[Structure],
                       staticDelay: Option[Int],
                       comb: Boolean
                     ): (Group, List[Structure]) = {

      assert(
        !(comb && staticDelay.isDefined && staticDelay.get != 0),
        s"Combinational group has delay: ${staticDelay.get}"
      )

      val (connections, st) = structure.partitionMap[Assign, Structure] {
        case c: Assign => Left(c)
        case s => Right(s)
      }

      (this (id, connections, if (comb) None else staticDelay, comb), st)
    }
  }

  case class CompInst(id: String, args: List[BigInt])

  sealed trait GuardExpr
  case class Atom(item: Port) extends GuardExpr

  object Atom {
    def apply(item: Port): GuardExpr = item match {
      case ConstantPort(1, v) if v == 1 => True
      case _ => new Atom(item)
    }
  }

  case class And(left: GuardExpr, right: GuardExpr) extends GuardExpr

  case class Or(left: GuardExpr, right: GuardExpr) extends GuardExpr

  case class Not(inner: GuardExpr) extends GuardExpr

  case object True extends GuardExpr

  /** *** control **** */
  sealed trait Control {
    var attributes: Map[String, Int] = Map[String, Int]()

    def seq(c: Control): Control = (this, c) match {
      case (Empty, c) => c
      case (c, Empty) => c
      case (seq0: SeqComp, seq1: SeqComp) => SeqComp(seq0.stmts ++ seq1.stmts)
      case (seq: SeqComp, _) => SeqComp(seq.stmts ++ List(c))
      case (_, seq: SeqComp) => SeqComp(this :: seq.stmts)
      case _ => SeqComp(List(this, c))
    }

    def par(c: Control): Control = (this, c) match {
      case (Empty, c) => c
      case (c, Empty) => c
      case (par0: ParComp, par1: ParComp) => ParComp(par0.stmts ++ par1.stmts)
      case (par0: ParComp, par1) => ParComp(par0.stmts ++ List(par1))
      case (par0, par1: ParComp) => ParComp(par0 :: par1.stmts)
      case _ => ParComp(List(this, c))
    }
  }

  case class SeqComp(stmts: List[Control]) extends Control

  case class ParComp(stmts: List[Control]) extends Control

  case class If(port: Port, cond: CompVar, trueBr: Control, falseBr: Control)
    extends Control

  case class While(port: Port, cond: CompVar, body: Control) extends Control

  case class Enable(id: CompVar) extends Control

  case class Invoke(
                     id: CompVar,
                     refCells: List[(String, CompVar)],
                     inConnects: List[(String, Port)],
                     outConnects: List[(String, Port)]
                   ) extends Control

  case object Empty extends Control
}
