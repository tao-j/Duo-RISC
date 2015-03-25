import Chisel._

class RegisterFileLink extends Bundle()
{
  val rs1_addr = UInt(INPUT, 5)
  val rs1_data = Bits(OUTPUT, 32)
  val rs2_addr = UInt(INPUT, 5)
  val rs2_data = Bits(OUTPUT, 32)

  val waddr    = UInt(INPUT, 5)
  val wdata    = Bits(INPUT, 32)
  val wen      = Bool(INPUT)
}

class RegisterFile extends Module
{
  val io = new RegisterFileLink()

  val regfile = Mem(Bits(width = 32), 32)

  when (io.wen && (io.waddr != UInt(0)))
  {
    regfile(io.waddr) := io.wdata
  }

  io.rs1_data := Mux((io.rs1_addr != UInt(0)), regfile(io.rs1_addr), UInt(0, 32))
  io.rs2_data := Mux((io.rs2_addr != UInt(0)), regfile(io.rs2_addr), UInt(0, 32))

}
