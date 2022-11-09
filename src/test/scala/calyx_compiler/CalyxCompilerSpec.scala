package calyx_compiler

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class CalyxCompilerSpec extends AnyFreeSpec with ChiselScalatestTester {

  "Compiles a list of commands to Calyx IR" in {
    test(new Queue(UInt(32.W), 16)) { dut =>
      val program: Command[Bool] = for {
        _ <- Poke(dut.io.enq.valid, 1.B)
        _ <- Step(1)
        p <- Peek(dut.io.deq.valid)
      } yield p
      val expectedCalyx = Seq(
        List[Group[Data]](
          new Group(
            Poke(dut.io.enq.valid, 1.B), dut.clock
          ),
          new Group(
            Peek(dut.io.enq.valid), dut.clock
          ),
          new Group(
            Return(dut.io.enq.valid), dut.clock
          )
        )
      )
      assert(CalyxCompiler.compileCommand(program, dut.clock) == expectedCalyx)
    }
  }
}