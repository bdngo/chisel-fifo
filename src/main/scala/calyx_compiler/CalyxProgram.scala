package calyx_compiler

case class Port(name: String, width: Int)

trait CalyxComponent

sealed trait CalyxPrimitive extends CalyxComponent
case class Reg(width: Int) extends CalyxPrimitive
case class Add(width: Int) extends CalyxPrimitive

abstract class CalyxUserComponent extends CalyxComponent {
  val inputPorts: Seq[Port]
  val outputPorts: Seq[Port]
}

case class Cell(name: String, component: CalyxComponent)

case class Assignment(lhs: String, rhs: String)
case class Group(assns: Seq[Assignment])
case class Wires(topLevelAssns: Seq[Assignment], groups: Seq[Group])

// case class CalyxProgram {
//
// }
