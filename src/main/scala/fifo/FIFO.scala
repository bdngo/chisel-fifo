package fifo

import chisel3._
import chisel3.util._

class FIFO(val width: Int = 8, val depth: Int = 32) extends Module {
  val io = IO(new Bundle {
    val wrEn = Input(Bool())
    val dIn = Input(UInt(width.W))
    val full = Output(Bool())

    val rdEn = Input(Bool())
    val dOut = Output(UInt(width.W))
    val empty = Output(Bool())
  })

  val buffer = Wire(Vec(depth, UInt(width.W)))
  val ptrWidth = log2Ceil(depth)

  val rdPtr = RegInit(0.U(ptrWidth.W))
  val wrPtr = RegInit(0.U(ptrWidth.W))

  var rdWrap = RegInit(false.B)
  var wrWrap = RegInit(false.B)

  io.full := (rdPtr === wrPtr) && (rdWrap =/= wrWrap)
  io.empty := (rdPtr === wrPtr) && (rdWrap === wrWrap)

  when (io.wrEn && !io.full) {
    when (wrPtr === (width - 1).U) {
      wrWrap = ~wrWrap
    }
    wrPtr := wrPtr + 1.U
    buffer(wrPtr) := io.dIn
  }.elsewhen (io.rdEn && !io.empty) {
    when (rdPtr === (width - 1).U) {
      rdWrap = ~rdWrap
    }
    rdPtr := rdPtr + 1.U
    io.dOut := buffer(rdPtr)
  }
}
