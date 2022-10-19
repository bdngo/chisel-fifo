package interpreter

import chisel3._
import chisel3.util._
import chiseltest._

sealed trait Command[R]
case class Peek[I <: Data](signal: I) extends Command[I]
case class Poke[I <: Data](signal: I, value: I) extends Command[I]
case class Step(cycles: Int) extends Command[Unit]

case class Return[R](retval: R) extends Command[R]
case class Cont[R1, R2](c: Command[R1], next: R1 => Command[R2]) extends Command[R2]

object Interpreter {
  def flatMap[R1, R2](c: Command[R1], f: R1 => Command[R2]): Command[R2] = {
    c match {
      case Return(retval)   => f(retval)
      case cmd: Command[R1] => Cont(cmd, f)
    }
  }

  def run[R](c: Command[R], clock: Clock): R = {
    c match {
      case Peek(signal)        => signal.peek() // same for poke, step
      case Poke(signal, value) => signal.poke(value)
      case Step(cycles)        => clock.step(cycles)
      case Return(retval)      => retval
      case Cont(c1, c2) => {
        val r = run(c1, clock)
        run(c2(r), clock)
      }
    }
  }
}