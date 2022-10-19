package interpreter

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala.collection.mutable.ListBuffer

class InterpreterSpec extends AnyFreeSpec with ChiselScalatestTester {

  "FIFO should enqueue data" in {
    test(new Queue(UInt(32.W), 16)) { dut =>
      val program: Command[Boolean] = for {
        _ <- Poke(dut.io.enq.valid, 1.B)
        _ <- Step(1)
        p <- Peek(dut.io.deq.valid)
      } yield p.litToBoolean

      val allGood = Interpreter.runCommand(program, dut.clock)
      assert(allGood)
    }
  }
}