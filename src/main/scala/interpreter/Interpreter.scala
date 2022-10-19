package interpreter

import chisel3._
import chisel3.util._
import chiseltest._

sealed trait Command[+R] {
  def flatMap[R2](f: R => Command[R2]): Command[R2] = {
    this match {
      case Return(retval)   => f(retval)
      case cmd: Command[R] => Cont(cmd, f)
    }
  }

  def map[R2](f: R => R2): Command[R2] = {
    this.flatMap { c =>
      Return(f(c))
    }
  }
}
case class Peek[I <: Data](signal: I) extends Command[I]
case class Poke[I <: Data](signal: I, value: I) extends Command[Unit]
case class Step(cycles: Int) extends Command[Unit]

case class Return[R](retval: R) extends Command[R]
case class Cont[R1, R2](c: Command[R1], next: R1 => Command[R2]) extends Command[R2]

object Interpreter {
  def runCommand[R](c: Command[R], clock: Clock): R = {
    println(c)
    c match {
      case peek @ Peek(signal)        => runPeek(peek)
      case poke @ Poke(signal, value) => runPoke(poke).asInstanceOf[R]
      case Step(cycles)               => clock.step(cycles).asInstanceOf[R]
      case Return(retval)             => retval
      case Cont(c1, c2) =>
        val r = runCommand(c1, clock)
        runCommand(c2(r), clock)
    }
  }

  def runPeek[R <: Data](c: Peek[R]): R = {
      c.signal.peek()
  }

  def runPoke[R <: Data](c: Poke[R]): Unit = {
    c.signal.poke(c.value)
  }
}