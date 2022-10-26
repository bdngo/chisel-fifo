package fifo

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala.collection.mutable.ListBuffer

class FIFOSpec extends AnyFreeSpec with ChiselScalatestTester {

  "FIFO should enqueue data" in {
    test(new FIFO(8, 32)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val testValues = for { x <- 0 until 32} yield x + 10
      // test init state
      dut.io.empty.expect(true.B)
      dut.io.full.expect(false.B)

      dut.clock.step(1)
      for (i <- 0 until 31) {
        dut.io.wrEn.poke(true.B)
        dut.io.dIn.poke(testValues(i).U)

        dut.io.empty.expect(false.B)
        dut.io.full.expect(false.B)
        dut.clock.step(1)
      }

      dut.io.wrEn.poke(true.B)
      dut.io.dIn.poke(testValues(31).U)
      dut.io.wrEn.poke(false.B)
      dut.io.empty.expect(false.B)
      dut.io.full.expect(true.B)

      dut.clock.step(10)
      dut.io.empty.expect(false.B)
      dut.io.full.expect(true.B)

      for (i <- 0 until 20) {
        dut.io.wrEn.poke(true.B)
        dut.io.dIn.poke(5.U)

        dut.io.empty.expect(false.B)
        dut.io.full.expect(true.B)
      }

      dut.clock.step(5)
      val receivedValues = new ListBuffer[UInt]()
      for (i <- 0 until 31) {
        dut.io.rdEn.poke(true.B)
        receivedValues += dut.io.dOut.peek()

        dut.io.empty.expect(false.B)
        dut.io.full.expect(false.B)
        dut.clock.step(1)
      }

      dut.io.rdEn.poke(true.B)
      receivedValues += dut.io.dOut.peek()
      dut.io.wrEn.poke(false.B)
      dut.io.empty.expect(true.B)
      dut.io.full.expect(false.B)

      dut.clock.step(10)
      dut.io.empty.expect(true.B)
      dut.io.full.expect(false.B)

      val receivedValuesLst = receivedValues.toList
      assert(testValues == receivedValuesLst)

      dut.io.rdEn.poke(true.B)
      receivedValues(0) = dut.io.dOut.peek()
      dut.io.wrEn.poke(false.B)
      dut.io.empty.expect(true.B)
      dut.io.full.expect(false.B)
    }
  }
}
