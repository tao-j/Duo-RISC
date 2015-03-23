import Chisel._

class PeripheralLink extends Bundle {
  val x = UInt(INPUT, 16)
}
class PeripheralIO extends Bundle {
  val segment_display_sub_io = new SegmentDisplaySubIO()
}
class Peripheral extends Module{
  val io = new Bundle() {
    val in = new PeripheralLink
    val out = new PeripheralIO
  }
  val segment_display = Module(new SegmentDisplay)
  segment_display.io.x <> io.in.x
  segment_display.io.sub_io <> io.out.segment_display_sub_io
}


class SegmentDisplaySubIO extends Bundle {
  val segment = UInt(OUTPUT, width = 7)
  val dot = UInt(OUTPUT, width = 1)
  val select = UInt(OUTPUT, width = 4)
}
class SegmentDisplay extends Module {
  val io = new Bundle {
    val x = UInt(INPUT, 16)
    val sub_io = new SegmentDisplaySubIO
  }

  io.sub_io.select := UInt("b1111")
  io.sub_io.segment := UInt("b111_1111")
  io.sub_io.dot := UInt("b1")

  val counter = Reg(init = UInt(0, 19))
  counter := counter + UInt(1)

  val digit = UInt()
  digit := UInt("h_F")
  switch (counter(18,17)) {
    is(UInt(0)) { digit := io.x( 3, 0); io.sub_io.select := UInt("b1110") }
    is(UInt(1)) { digit := io.x( 7, 4); io.sub_io.select := UInt("b1101") }
    is(UInt(2)) { digit := io.x(11, 8); io.sub_io.select := UInt("b1011") }
    is(UInt(3)) { digit := io.x(15,12); io.sub_io.select := UInt("b0111") }
  }
  switch (digit) {
    is(UInt("h_0")) { io.sub_io.segment := UInt("b1000000") }
    is(UInt("h_1")) { io.sub_io.segment := UInt("b1111001") }
    is(UInt("h_2")) { io.sub_io.segment := UInt("b0100100") }
    is(UInt("h_3")) { io.sub_io.segment := UInt("b0110000") }
    is(UInt("h_4")) { io.sub_io.segment := UInt("b0011001") }
    is(UInt("h_5")) { io.sub_io.segment := UInt("b0010010") }
    is(UInt("h_6")) { io.sub_io.segment := UInt("b0000010") }
    is(UInt("h_7")) { io.sub_io.segment := UInt("b1111000") }
    is(UInt("h_8")) { io.sub_io.segment := UInt("b0000000") }
    is(UInt("h_9")) { io.sub_io.segment := UInt("b0010000") }
    is(UInt("h_A")) { io.sub_io.segment := UInt("b0001000") }
    is(UInt("h_B")) { io.sub_io.segment := UInt("b0000011") }
    is(UInt("h_C")) { io.sub_io.segment := UInt("b1000110") }
    is(UInt("h_D")) { io.sub_io.segment := UInt("b0100001") }
    is(UInt("h_E")) { io.sub_io.segment := UInt("b0000110") }
    is(UInt("h_F")) { io.sub_io.segment := UInt("b0001110") }
  }
}
