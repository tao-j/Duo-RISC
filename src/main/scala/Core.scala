import Chisel._

class CoreIO() extends Bundle
{
  val mem = new MemoryIO().flip()
  val reset = Bool(INPUT)
}

class Core() extends Module {
  val io = new CoreIO()

  val s_if :: s_id :: s_exe :: s_mem :: s_wb :: Nil = Enum(UInt(width = 3), 5)

  // registers
  val pc = Reg(init = UInt(0, 32))
  // NOTE:
  val pc_start = UInt("h3000")
  val ir = Reg(init = UInt(0, 32))
  val c = Reg(init = UInt(0, 32)) // pseudo ALU output
  val d = Reg(init = UInt(0, 32)) // data from datapath
  val state = Reg(init = s_if)

  // instruction format
  val opcode = ir(31, 26)
  val rs = ir(25, 21)
  val rt = ir(20, 16)
  val rd = ir(15, 11)
  val sa = ir(10, 6)
  val func = ir(5, 0)
  val imm = ir(15, 0)
  val addr = ir(25, 0)
  val sign = ir(15)
  val s_imm = Cat(Fill(sign, 16), imm)
  // sign-ext. imm
  val z_imm = Cat(Fill(UInt(0), 16), imm) // zero-ext. imm

  // register values from regfile
  val rs_v = UInt()
  val rt_v = UInt()

  // for next_pc
  val p4_addr = pc + UInt(4)
  // pc+4
  val br_addr = pc + {
    Cat(s_imm(29, 0), UInt(0, 2))
  }
  // beq, bne
  val jr_addr = rs_v
  // jr
  val jp_addr = Cat(pc(31, 28), addr, UInt(0, 2)) // j, jal

  // instruction decode
  val i_add = Bool()
  val i_sub = Bool()
  val i_and = Bool()
  val i_or = Bool()
  val i_xor = Bool()
  val i_slt = Bool()
  val i_sll = Bool()
  val i_srl = Bool()
  val i_sra = Bool()
  val i_jr = Bool()
  val i_addi = Bool()
  val i_andi = Bool()
  val i_ori = Bool()
  val i_xori = Bool()
  val i_lw = Bool()
  val i_sw = Bool()
  val i_beq = Bool()
  val i_bne = Bool()
  val i_lui = Bool()
  val i_j = Bool()
  val i_jal = Bool()
  i_add := (opcode === UInt("h00")) & (func === UInt("h20")) // add
  i_sub := (opcode === UInt("h00")) & (func === UInt("h22")) // sub
  i_and := (opcode === UInt("h00")) & (func === UInt("h24")) // and
  i_or := (opcode === UInt("h00")) & (func === UInt("h25")) // or
  i_xor := (opcode === UInt("h00")) & (func === UInt("h26")) // xor
  i_slt := (opcode === UInt("h00")) & (func === UInt("h2a")) // slt // NOTE: added
  i_sll := (opcode === UInt("h00")) & (func === UInt("h00")) // sll
  i_srl := (opcode === UInt("h00")) & (func === UInt("h02")) // srl
  i_sra := (opcode === UInt("h00")) & (func === UInt("h03")) // sra
  i_jr := (opcode === UInt("h00")) & (func === UInt("h08")) // jr
  i_addi := opcode === UInt("h08") // addi
  i_andi := opcode === UInt("h0c") // andi
  i_ori := opcode === UInt("h0d") // ori
  i_xori := opcode === UInt("h0e") // xori
  i_lw := opcode === UInt("h23") // lw
  i_sw := opcode === UInt("h2b") // sw
  i_beq := opcode === UInt("h04") // beq
  i_bne := opcode === UInt("h05") // bne
  i_lui := opcode === UInt("h0f") // lui
  i_j := opcode === UInt("h02") // j
  i_jal := opcode === UInt("h03")
  // jal

  // will be nets (wires):
  val wpc = Bool()  // 0. write PC
  val wir = Bool()  // 1. write IR
  val wreg = Bool() // 2. write register file
  val wmem = Bool() // 3. write memory
  val next_pc = UInt()// 4. next state
  val c_in = UInt()// 5. to register C
  val data_2_rf = UInt()// 6. select ALU output
  val dest_rn = UInt()// 7. destination register
  val next_state = UInt()// 8. next state
  val r_m_addr = UInt() // 9. read memory address
  val w_m_addr = UInt() // 10. write memory address
  io.mem.r_m_addr := r_m_addr
  io.mem.w_m_addr := w_m_addr

  when(io.reset === Bool(true)) {
    pc := pc_start
  }
    .elsewhen(wpc) {
    pc := next_pc
  }

  when(io.reset === Bool(true)) {
    ir := UInt(0)
  }
    .elsewhen(wir) {
    ir := io.mem.d_f_mem
  }

  when(io.reset === Bool(true)) {
    c := UInt(0)
    d := UInt(0)
    state := s_if
  }
    .otherwise {
    c := c_in
    d := io.mem.d_f_mem
    state := next_state
  }

  // register file
  val regfile = Mem(UInt(width = 32), 32)
  rs_v := regfile(rs)
  rt_v := regfile(rt)
  when(io.reset === Bool(x = true)) {
    for (i <- 0 until 32) {
      regfile(i) := UInt(0)
    }
  }
    .elsewhen(wreg && (dest_rn != UInt(0))) {
    regfile(dest_rn) := data_2_rf
  }

  // memory-mapped I/O
  val io_space = c(31) & // i/o space:
    ~c(30) & // a000_0000 -
    c(29)
  // bfff_ffff

  // vram space, can be done outside of cpu
  val vr_space = c(31) & // vram space:
    c(30) & // c000_0000 -
    ~c(29) // dfff_ffff

  // output signals
  io.mem.w_d_mem := (state === s_mem) & i_sw & ~io_space & ~vr_space // write data memory
  io.mem.d_t_mem := rt_v // write reg(rt) to memory
  //io_rdn  := ~(s_mem & i_lw &  io_space)            // i/o read (active low)
  //wr_vram :=   s_mem & i_sw &  vr_space             // write  video ram
  //rd_vram :=   s_mem & i_lw &  vr_space             // read   video ram


  wpc := Bool(false)// 0. write PC
  wir := Bool(false)// 1. write IR
  wreg := Bool(false)// 2. write register file
  wmem := Bool(false)// 3. write memory
  next_pc := p4_addr // 4. next pc
  c_in := UInt(0, 32)// 5. to register C
  data_2_rf := c // 6. select ALU output
  dest_rn := rd // 7. destination register
  next_state := s_if // 8. next state
  r_m_addr := pc // 9. read memory address
  w_m_addr := UInt("h_a_dead_bee") // 10. write memory address
    //---------------------------------------- IF:
  when (state === s_if) {
//    printf("s_if\n")
    // TODO: wait data till valid
    wpc := Bool(true) // write PC
    wir := Bool(true) // write IR
    next_state := s_id // next is(state) ID
  } //--------------------------------------------- ID:
  when (state === s_id) { // ID state
//    printf("s_id\n")
    when(i_j) { // j   -> IF
      wpc := Bool(x = true) // write PC
      next_pc := jp_addr // jump address
      next_state := s_if // next state: IF
    }
      .elsewhen(i_jal) { // jal -> IF
      wpc := Bool(x = true) // write PC
      next_pc := jp_addr // jump address
      wreg := Bool(x = true) // save return address
      dest_rn := UInt("d31", 5) // to $31
      data_2_rf := pc // return address
      next_state := s_if // next state: IF
    }
      .elsewhen(i_jr) { // jr  -> when
      wpc := Bool(x = true) // write PC
      next_pc := jr_addr // jump address
      next_state := s_if // next state: IF
    }
      .otherwise { // other instructions
      c_in := br_addr // branch address
      next_state := s_exe // others -> EXE
    }
  } //--------------------------------------------- EXE:
  when (state === s_exe) { // EXE state
//    printf("s_exe\n")
    when(i_add) {  // add  -> WB
      c_in := rs_v + rt_v
      next_state := s_wb
    }
    when(i_sub) {  // sub  -> WB
      c_in := rs_v - rt_v
      next_state := s_wb
    }
    when(i_and) {  // and  -> WB
      c_in := rs_v & rt_v
      next_state := s_wb
    }
    when(i_or) {  // or   -> WB
      c_in := rs_v | rt_v
      next_state := s_wb
    }
    when(i_xor) {  // xor  -> WB
      c_in := rs_v ^ rt_v
      next_state := s_wb
    }
    when(i_slt) {  // slt  -> WB
      c_in := (rs_v < rt_v).toUInt()
      next_state := s_wb
    }
    when(i_sll) {  // sll  -> WB
      c_in := (rt_v << sa)(31,0)
      next_state := s_wb
    }
    when(i_srl) {  // srl  -> WB
      c_in := rt_v >> sa
      next_state := s_wb
    }
    when(i_sra) {  // sra  -> WB
      c_in := (rt_v.toSInt() >> sa).toUInt()
      next_state := s_wb
    }
    when(i_addi) {  // addi -> WB
      c_in := rs_v + s_imm
      next_state := s_wb
    }
    when(i_andi) {  // andi -> WB
      c_in := rs_v & z_imm
      next_state := s_wb
    }
    when(i_ori) {  // ori  -> WB
      c_in := rs_v | z_imm
      next_state := s_wb
    }
    when(i_xori) {  // xori -> WB
      c_in := rs_v ^ z_imm
      next_state := s_wb
    }
    when(i_lw) {  // lw  -> MEM
      c_in := rs_v + s_imm
      next_state := s_mem
    }
    when(i_sw) {  // sw  -> MEM
      c_in := rs_v + s_imm
      next_state := s_mem
    }
    when(i_beq) {  // beq  -> IF
      when(rs_v === rt_v) {
        next_pc := br_addr
        wpc := Bool(x = true)
      }
      next_state := s_if
    }
    when(i_bne) {  // bne  -> IF
      when(rs_v != rt_v) {
        next_pc := br_addr
        wpc := Bool(x = true)
      }
      next_state := s_if
    }
    when(i_lui) {  // lui  -> WB
      c_in := Cat(imm, UInt(0, 16))
      next_state := s_wb
    }
  } //--------------------------------------------- MEM:
  when (state === s_mem) {  // MEM state
    when(i_lw) {  // lw -> WB
      r_m_addr := c // read data address
      next_state := s_wb // next is(state) WB
    } otherwise {  // sw -> IF
      w_m_addr := c // write data address
      wmem := Bool(true) // write memory
      next_state := s_if // next state: IF
    }
  } //--------------------------------------------- WB:
  when (state === s_wb) { // WB state
    wreg := Bool(true) // write register file
    next_state := s_if // next state: IF
    when(i_lw) {
      data_2_rf := d // select memory data
    }
    when(i_lw || i_addi || i_andi || i_ori || i_xori || i_lui) {
      dest_rn := rt // destination is(reg) rt
      }
    } //--------------------------------------------- END

  val ticks = Reg(init = UInt(0, 32))
  unless (io.reset) {
    ticks := ticks + UInt(1)
  }

}