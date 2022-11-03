package fifo

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala.collection.mutable.ListBuffer

class FIFOSpec extends AnyFreeSpec with ChiselScalatestTester {

  "FIFO should enqueue data" in {
    test(new FIFO(8, 32)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val testValues = for { x <- 0 until 32} yield (x + 10).U(8.W)
      // test init state
      dut.io.empty.expect(true.B)
      dut.io.full.expect(false.B)

      dut.clock.step(1)
      for (i <- 0 until 31) {
        dut.io.wrEn.poke(true.B)
        dut.io.dIn.poke(testValues(i))

        dut.clock.step(1)
        dut.io.empty.expect(false.B)
        dut.io.full.expect(false.B)
      }

      dut.io.wrEn.poke(true.B)
      dut.io.dIn.poke(testValues(31))
      dut.clock.step(1)
      dut.io.empty.expect(false.B)
      dut.io.full.expect(true.B)

      dut.clock.step(10)
      dut.io.empty.expect(false.B)
      dut.io.full.expect(true.B)

      for (_ <- 0 until 20) {
        dut.io.wrEn.poke(true.B)
        dut.io.dIn.poke(5.U)

        dut.io.empty.expect(false.B)
        dut.io.full.expect(true.B)
      }
      dut.io.wrEn.poke(false.B)

      dut.clock.step(5)
      val receivedValues = new ListBuffer[UInt]()
      for (_ <- 0 until 31) {
        dut.io.rdEn.poke(true.B)
        receivedValues += dut.io.dOut.peek()

        dut.clock.step(1)
        dut.io.empty.expect(false.B)
        dut.io.full.expect(false.B)
      }

      dut.io.rdEn.poke(true.B)
      receivedValues += dut.io.dOut.peek()
      dut.clock.step(1)
      dut.io.empty.expect(true.B)
      dut.io.full.expect(false.B)

      dut.clock.step(10)
      dut.io.empty.expect(true.B)
      dut.io.full.expect(false.B)

      println(testValues)
      println(receivedValues)
      testValues.zip(receivedValues).foreach{ case (test, recv) => assert(test.litValue == recv.litValue) }
      //assert(testValues == receivedValues)

      dut.io.rdEn.poke(true.B)
      receivedValues(0) = dut.io.dOut.peek()
      dut.io.wrEn.poke(false.B)
      dut.io.empty.expect(true.B)
      dut.io.full.expect(false.B)
    }
  }
}
