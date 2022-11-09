package calyx_compiler

import chisel3._
import chisel3.util._
import chiseltest._

import scala.collection.mutable.ListBuffer

  object CalyxCompiler {
    trait Synthesizable {
      def synthesize(): CalyxProgram
    }
    def runHLS(c: Synthesizable): CalyxProgram = {
      ???
    }
  }

sealed trait Command[+R <: Data] {
  def flatMap[R2 <: Data](f: R => Command[R2]): Command[R2] = {
    this match {
      case Return(retval)  => f(retval)
      case cmd: Command[R] => Cont(cmd, f)
    }
  }

  def map[R2 <: Data](f: R => R2): Command[R2] = {
    this.flatMap { c =>
      Return(f(c))
    }
  }
}
case class Peek[I <: Data](signal: I) extends Command[I]
case class Poke[I <: Data](signal: I, value: I) extends Command[I]
case class Step(cycles: Int) extends Command[Data]

case class Return[R <: Data](retval: R) extends Command[R]
case class Cont[R1 <: Data, R2 <: Data](c: Command[R1], next: R1 => Command[R2]) extends Command[R2]

class Group[R <: Data](val cmd: Command[R], val clk: Clock)

sealed trait Control[+R <: Data]
case class CalyxSeq[R <: Data](groups: List[Group[R]]) extends Control[R]

object CalyxCompiler {

  def runCommand[R <: Data](c: Command[R], clock: Clock): R = {
    println(c)
    c match {
      case peek @ Peek(signal)        => runPeek(peek)
      case poke @ Poke(signal, value) => runPoke(poke).asInstanceOf[R]
      case Step(cycles)               =>
        clock.step(cycles)
        true.asInstanceOf[R]
      case Return(retval)             => retval
      case Cont(c1, c2) =>
        val r = runCommand(c1, clock)
        runCommand(c2(r), clock)
    }
  }

  def runPeek[R <: Data](c: Peek[R]): R = {
      c.signal.peek()
  }

  def runPoke[R <: Data](c: Poke[R]): Bool = {
    c.signal.poke(c.value)
    true.B
  }

  def processGroup[R <: Data](g: Group[R]): R = {
    runCommand(g.cmd, g.clk)
  }

  def splitCommand[R <: Data](c: Command[R], acc: ListBuffer[Command[R]], clock: Clock): ListBuffer[Command[R]] = {
    c match {
      case c: Cont[R, _] =>
        val c1 = c.c
        val next = c.next
        val r = runCommand(c1, clock)
        val nextCmd = next(r) // will be cont or return
        nextCmd match {
          case Cont(c2, nextNext) =>
            c2 match {
              case Step(_) =>
                val r = runCommand(c2, clock)
                val last = acc.last
                acc.last = Cont(last, _ => Return(r))
                val nextNextCmd = nextNext(r)
                ListBuffer.concat(acc, splitCommand(nextNextCmd, ListBuffer.empty, clock))
              case _ => splitCommand(nextCmd, acc :+ c, clock)
            }
          case _ => splitCommand(nextCmd, acc, clock)
        }
      case Return(_) => ListBuffer.concat(acc, ListBuffer(c))
    }
  }

  def compileCommand[R <: Data](c: Command[R], clock: Clock): Control[R] = {
    val split = splitCommand(c, List.empty, clock)
    Seq(split.map(x => new Group(x, clock)))
  }
}