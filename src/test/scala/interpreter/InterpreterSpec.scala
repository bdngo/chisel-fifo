package fifo

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala.collection.mutable.ListBuffer

class FIFOSpec extends AnyFreeSpec with ChiselScalatestTester {
  "FIFO should enqueue data" in {
    val program: Command[Boolean] = for {
        _ <- Poke(dut.enq.valid, 1.B)
        _ <- Step(1)
        p <- Peek(dut.deq.valid)
    } yield p.litValue == 1
    test(new Queue(UInt(32.W), 16)) { dut =>
        val allGood = run(program, dut.clock)
        assert(allGood)
    }