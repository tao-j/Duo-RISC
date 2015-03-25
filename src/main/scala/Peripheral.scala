import Chisel._

/* steps to add a peripheral device
 1. declare DeviceName DeviceNameRaw DeviceNamePin class
 2. add the above Raw/Pin to the PeripheralLink/IO
 3. create instances under Peripheral class
 4. interconnect instances.pin/link to Peripheral.io.pin_hub/link_hub
 5. PeripheralIO is automatically connected to the TopIO
*/
class PeripheralLink extends Bundle {
  // TODO: a temporarily bypassed version of data link, following lines should be inside the class, see below
  val segment_display_raw = UInt(INPUT, 16)
  val analog_monitor_raw = new AnalogMonitorRaw()
}
class PeripheralIO extends Bundle {
  val segment_display_pin = new SegmentDisplayPin()
  val analog_monitor_pin = new AnalogMonitorPin()
}
class Peripheral extends Module{
  val io = new Bundle() {
    val raw_hub = new PeripheralLink
    val pin_hub = new PeripheralIO
  }
  // TODO: interpreting data from PeripheralLink and assign following values
  // val segment_display_raw = UInt(0, 16)
  // val analog_monitor_raw = new AnalogMonitorRaw()

  val segment_display = Module(new SegmentDisplay)
  segment_display.io.pin <> io.pin_hub.segment_display_pin
  segment_display.io.raw <> io.raw_hub.segment_display_raw

  val v_clk_25m = new Clock(this.reset)

  //TODO: change to v_clk
  val analog_monitor = Module(new AnalogMonitor(this.clock))
  analog_monitor.io.pin <> io.pin_hub.analog_monitor_pin
  analog_monitor.io.raw <> io.raw_hub.analog_monitor_raw
}


class SegmentDisplayPin extends Bundle {
  val segment = UInt(OUTPUT, width = 7)
  val select = UInt(OUTPUT, width = 4)
  val dot = UInt(OUTPUT, width = 1)
}
class SegmentDisplay extends Module {
  val io = new Bundle {
    val raw = UInt(INPUT, 16)
    val pin = new SegmentDisplayPin
  }

  io.pin.select := UInt("b1111")
  io.pin.segment := UInt("b111_1111")
  io.pin.dot := UInt("b1")

  val counter = Reg(init = UInt(0, 19))
  counter := counter + UInt(1)

  val digit = UInt()
  digit := UInt("h_F")
  switch (counter(18,17)) {
    is(UInt(0)) { digit := io.raw( 3, 0); io.pin.select := UInt("b1110") }
    is(UInt(1)) { digit := io.raw( 7, 4); io.pin.select := UInt("b1101") }
    is(UInt(2)) { digit := io.raw(11, 8); io.pin.select := UInt("b1011") }
    is(UInt(3)) { digit := io.raw(15,12); io.pin.select := UInt("b0111") }
  }
  switch (digit) {
    is(UInt("h_0")) { io.pin.segment := UInt("b1000000") }
    is(UInt("h_1")) { io.pin.segment := UInt("b1111001") }
    is(UInt("h_2")) { io.pin.segment := UInt("b0100100") }
    is(UInt("h_3")) { io.pin.segment := UInt("b0110000") }
    is(UInt("h_4")) { io.pin.segment := UInt("b0011001") }
    is(UInt("h_5")) { io.pin.segment := UInt("b0010010") }
    is(UInt("h_6")) { io.pin.segment := UInt("b0000010") }
    is(UInt("h_7")) { io.pin.segment := UInt("b1111000") }
    is(UInt("h_8")) { io.pin.segment := UInt("b0000000") }
    is(UInt("h_9")) { io.pin.segment := UInt("b0010000") }
    is(UInt("h_A")) { io.pin.segment := UInt("b0001000") }
    is(UInt("h_B")) { io.pin.segment := UInt("b0000011") }
    is(UInt("h_C")) { io.pin.segment := UInt("b1000110") }
    is(UInt("h_D")) { io.pin.segment := UInt("b0100001") }
    is(UInt("h_E")) { io.pin.segment := UInt("b0000110") }
    is(UInt("h_F")) { io.pin.segment := UInt("b0001110") }
  }
}

class AnalogMonitorPin extends Bundle {
  val red = UInt(OUTPUT, 3)
  val green = UInt(OUTPUT, 3)
  val blue = UInt(OUTPUT, 2)
  val h_sync = Bool(OUTPUT)
  val v_sync = Bool(OUTPUT)
}
class AnalogMonitorRaw extends Bundle {
  val color = UInt(INPUT, 3 + 3 + 2)

  // TODO: using config file to specify width
  val x_coordinate = UInt(OUTPUT, 11)
  val y_coordinate = UInt(OUTPUT, 11)
  val video_enable = Bool(OUTPUT)
}
class AnalogMonitor(v_clk: Clock) extends Module(clock = v_clk) {
  val io = new Bundle {
    val pin = new AnalogMonitorPin()
    val raw = new AnalogMonitorRaw()
  }

  // TODO: using config file to specify width
  val x_i = Reg(init = SInt(0, 12)); val y_i = Reg(init = SInt(0, 12))
  val new_line = x_i === SInt(0)
  val new_field = y_i === SInt(0)
  io.pin.red := io.raw.color(7,5)
  io.pin.green := io.raw.color(4,2)
  io.pin.blue := io.raw.color(1,0)
  //wire clk_l; //clk_p pixel clock, clk_l line clock
  //60Hz 0 < x < 1023, 0 < y < 767 75Mhz clk_d
  //Horizontal (line) Front Porch 24clk_p Sync 136clk_p Back Porch 160clk_p = 1344
  //Vertical (field) 3clk_l 6clk_l 29clk_l = 806
  //60Hz 0 < x < 799, 0 < y < 599 40Mhz clk_d
  // parameter h_pixel = 'd799;
  // parameter v_pixel = 'd599;
  // parameter h_front_porch = 'd40;
  // parameter h_sync_pulse = 'd128;
  // parameter h_back_porch = 'd88;
  // parameter v_front_porch = 'd1;
  // parameter v_sync_pulse = 'd4;
  // parameter v_back_porch = 'd23;
  // parameter line = h_pixel + h_front_porch + h_sync_pulse + h_back_porch;
  // parameter field = v_pixel + v_front_porch + v_sync_pulse + v_back_porch;
  //60Hz 0 < x < 639, 0 < y < 479 25Mhz clk_d
  val h_pixel = SInt(639)
  val h_sync_pulse = SInt(96)
  val h_back_porch = SInt(48)
  val h_front_porch = SInt(16)
  val line = h_pixel + h_front_porch + h_sync_pulse + h_back_porch

  val v_pixel = SInt(479)
  val v_sync_pulse = SInt(2)
  val v_back_porch = SInt(33) //29
  val v_front_porch = SInt(10)
  val field = v_pixel + v_front_porch + v_sync_pulse + v_back_porch

  val counter = Reg(init = UInt(0, 2))
  counter := counter + UInt(1)

  when (andR(counter)) {
  //when (Bool(true)) {
    when (x_i >= line) {
      x_i := SInt(0)
      // TODO: change SInt(525) to field will generate wrong code
      when (y_i >= SInt(525)) {
        y_i := SInt(0)
      } .otherwise {
        y_i := y_i + SInt(1)
      }
    } .otherwise {
      x_i := x_i + SInt(1)
    }

    when (this.reset) {
      x_i := SInt(0); y_i := SInt(0)
    }
  }
  io.pin.h_sync := Mux(x_i >= h_sync_pulse, Bool(true), Bool(false))
  io.pin.v_sync := Mux(y_i >= v_sync_pulse, Bool(true), Bool(false))

  io.raw.video_enable :=
    Mux(x_i > SInt(144), Bool(true), Bool(false)) &&
    Mux(x_i <= SInt(784), Bool(true), Bool(false)) &&
    Mux(y_i > SInt(35), Bool(true), Bool(false)) &&
    Mux(y_i <= SInt(515), Bool(true), Bool(false))
  io.raw.x_coordinate := x_i - h_back_porch - h_sync_pulse
  io.raw.y_coordinate := y_i - v_back_porch - v_sync_pulse
  // __    ____________________________    ________________
  //   |__|                            |__|                  hs
  //   |96|   ______________________   |96|   _____________
  // ________|       1 line         |________| (next line)
  //   |96|48|-------- 640 ---------|16|96|48|
  //   | 144 |                      |  |
  //   |------------ 784 -----------|  |
  //   |-------------- 800 ------------|


  // __    ____________________________    ________________
  //   |__|                            |__|                  vs
  //   | 2|   ______________________   | 2|   _____________
  // ________|       1 frame        |________| (next frame)
  //   | 2|33|-------- 480 ---------|10| 2|33|
  //   |  35 |                      |  |
  //   |------------ 515 -----------|  |
  //   |-------------- 525 ------------|
}
