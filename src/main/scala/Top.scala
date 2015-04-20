import Chisel._

class Top extends Module {
  val io = new Bundle {
    val peripheral = new PeripheralIO
  }

  val tile = Module(new CorePipelined)

  val peripheral = Module(new Peripheral)
  io.peripheral <> peripheral.io.pin_hub
  peripheral.io.raw_hub.segment_display_raw :=  tile.io.debug(15,0)


  peripheral.io.raw_hub.analog_monitor_raw.color :=
    Mux (peripheral.io.raw_hub.analog_monitor_raw.video_enable,
      peripheral.io.raw_hub.analog_monitor_raw.x_coordinate +
        peripheral.io.raw_hub.analog_monitor_raw.y_coordinate,
      UInt(0))
}

class TopTests(c: Top) extends Tester(c) {

  reset(1)
  for (i <- 0 until 5)
    peekAt(c.tile.d_cache.mem, i)
  step (200)
  //TODO: val prv_pc = peek(c.core.pc)

  peekAt(c.tile.d_cache.mem, 4)


  peek(c.tile.io.debug)
  peek(c.peripheral.io.pin_hub.segment_display_pin.segment)

  //see

  def see: Unit = {
    //peek()
    //peekAt()
  }
}

object Top {
  def main(args: Array[String]): Unit = {
    args.foreach(arg => println(arg))
    chiselMainTest(args, () => Module(new Top())) {
      c => new TopTests(c) }
  }
}
