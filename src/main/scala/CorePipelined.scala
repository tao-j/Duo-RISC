import Chisel._

// TODO: MEMIFCE: put dmem imem outside the core, or not?
class CorePipelined extends Module{
  val io = new Bundle {
    val debug = UInt(OUTPUT)
  }

  val i_cache = Module (new ICache)
  val d_cache = Module (new DCache)

  val d_path  = Module (new DataPath)
  val c_path  = Module (new ControlPath)

  d_path.io.data <> c_path.io.data
  c_path.io.ctrl <> d_path.io.ctrl

  c_path.io.dmem <> d_cache.io
  d_path.io.dmem <> d_cache.io
  c_path.io.imem <> i_cache.io
  d_path.io.imem <> i_cache.io

  io.debug := d_cache.io.debug
}
