import Chisel._
import Common.Constants._
import Common.MIPSInstructions._

class ControlToDataLink extends Bundle()
{
  val dec_stall  = Bool(OUTPUT)    // stall IF/DEC stages (due to hazards) branch
  val full_stall = Bool(OUTPUT)    // stall entire pipeline (due to D$ misses)
  val exe_pc_sel = UInt(OUTPUT, 2)
  val br_type    = UInt(OUTPUT, 4)
  val if_kill    = Bool(OUTPUT)
  val dec_kill   = Bool(OUTPUT)
  val op1_sel    = UInt(OUTPUT, 2)
  val op2_sel    = UInt(OUTPUT, 4)
  val alu_fun    = UInt(OUTPUT, 4)
  val wb_sel     = UInt(OUTPUT, 2)
  val rf_wen     = Bool(OUTPUT)
  val mem_val    = Bool(OUTPUT)
  val mem_fcn    = Bits(OUTPUT, 2) // read or write
  // val mem_typ    = Bits(OUTPUT, 3) // TODO: memory bitmask
  // val csr_cmd    = UInt(OUTPUT, 2) // NOTE: perform a search to substitute back, they are everywhere
}

class ControlPathLink extends Bundle()
{
  val imem = new CachePort().flip
  val dmem = new CachePort().flip
  val data  = new DataToControlLink().flip()
  val ctrl  = new ControlToDataLink()
  //override def clone = { new CpathIo().asInstanceOf[this.type] }
}

class ControlPath extends Module {

  val io = new ControlPathLink()

  val mips_ctrl_signals =
    ListLookup(io.data.dec_inst,
      List(N, BR_N  , OP1_X  , OP2_X     , OEN_0, OEN_0, ALU_X   , WB_X  , REN_0, MEN_0, M_X  /*, MT_X, CSR.N*/),
      Array(   /* valid |  BR   |  op1   |   op2     |  R1  |  R2  |  ALU    |  wb   | rf   | mem  | mem  | mask |  csr  */
               /* inst  | type  | select |  select   |  oen |  oen |function |  sel  | wen  |  en  |  wr  | type |  cmd  */
        ADD_   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   , OEN_1, OEN_1, ALU_ADD , WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
        SUB_   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   , OEN_1, OEN_1, ALU_SUB , WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
        AND_   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   , OEN_1, OEN_1, ALU_AND , WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
        OR_    -> List(Y, BR_N  , OP1_RS1, OP2_RS2   , OEN_1, OEN_1, ALU_OR  , WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
        XOR_   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   , OEN_1, OEN_1, ALU_XOR , WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
        SLT_   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   , OEN_1, OEN_1, ALU_SLT , WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
      //SLLV   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   , OEN_1, OEN_1, ALU_SLL , WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
      //SRLV   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   , OEN_1, OEN_1, ALU_SRL , WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
      //SRAV   -> List(Y, BR_N  , OP1_RS1, OP2_RS2   , OEN_1, OEN_1, ALU_SRA , WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
        SLL_   -> List(Y, BR_N  , OP1_X  , OP2_RS2   , OEN_0, OEN_1, ALU_SLLI, WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
        SRL_   -> List(Y, BR_N  , OP1_X  , OP2_RS2   , OEN_0, OEN_1, ALU_SRLI, WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
        SRA_   -> List(Y, BR_N  , OP1_X  , OP2_RS2   , OEN_0, OEN_1, ALU_SRAI, WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
                                                                                                          /*             */
        JR_    -> List(Y, BR_JR , OP1_RS1, OP2_X     , OEN_1, OEN_0, ALU_X   , WB_X  , REN_0, MEN_0, M_X  /*, MT_X, CSR.N*/),
        JALR_  -> List(Y, BR_JR , OP1_RS1, OP2_X     , OEN_1, OEN_0, ALU_X   , WB_PC4, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/), // TODO: untested
                                                                                                          /*             */
        ADDI_  -> List(Y, BR_N  , OP1_RS1, OP2_SEXT  , OEN_1, OEN_0, ALU_ADD , WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/), // v
        ANDI_  -> List(Y, BR_N  , OP1_RS1, OP2_ZEXT  , OEN_1, OEN_0, ALU_AND , WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
        ORI_   -> List(Y, BR_N  , OP1_RS1, OP2_ZEXT  , OEN_1, OEN_0, ALU_OR  , WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/), // v
        XORI_  -> List(Y, BR_N  , OP1_RS1, OP2_ZEXT  , OEN_1, OEN_0, ALU_XOR , WB_ALU, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
                                                                                                          /*             */
        LW_    -> List(Y, BR_N  , OP1_RS1, OP2_SEXT  , OEN_1, OEN_0, ALU_ADD , WB_MEM, REN_1, MEN_1, M_XRD/*, MT_W, CSR.N*/), // v
        SW_    -> List(Y, BR_N  , OP1_RS1, OP2_SEXT  , OEN_1, OEN_1, ALU_ADD , WB_X  , REN_0, MEN_1, M_XWR/*, MT_W, CSR.N*/), // v
                                                                                                          /*             */
        BEQ_   -> List(Y, BR_EQ , OP1_RS1, OP2_RS2   , OEN_1, OEN_1, ALU_X   , WB_X  , REN_0, MEN_0, M_X  /*, MT_X, CSR.N*/),
        BNE_   -> List(Y, BR_NE , OP1_RS1, OP2_RS2   , OEN_1, OEN_1, ALU_X   , WB_X  , REN_0, MEN_0, M_X  /*, MT_X, CSR.N*/), // v
        LUI_   -> List(Y, BR_N  , OP1_X  , OP2_HIGH  , OEN_0, OEN_0, ALU_COPY_2,WB_ALU,REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/), // v
                                                                                                          /*             */
        J_     -> List(Y, BR_J  , OP1_X  , OP2_ADDR  , OEN_0, OEN_0, ALU_X   , WB_X  , REN_0, MEN_0, M_X  /*, MT_X, CSR.N*/),
        JAL_   -> List(Y, BR_J  , OP1_X  , OP2_ADDR  , OEN_0, OEN_0, ALU_X   , WB_P4R, REN_1, MEN_0, M_X  /*, MT_X, CSR.N*/),
                                                                                                          /*             */
        NOP_   -> List(N, BR_N  , OP1_X  , OP2_X     , OEN_0, OEN_0, ALU_X   , WB_X  , REN_0, MEN_0, M_X  /*, MT_X, CSR.N*/)
      ))
  val (cs_val_inst: Bool) :: cs_br_type :: cs_op1_sel :: cs_op2_sel :: (cs_rs1_oen: Bool) :: (cs_rs2_oen: Bool) :: cs0 = mips_ctrl_signals
  val cs_alu_fun :: cs_wb_sel :: (cs_rf_wen: Bool) :: (cs_mem_en: Bool) :: cs_mem_fcn /*:: cs_msk_sel :: cs_csr_cm*/ :: Nil = cs0

  // Branch Logic
  val ctrl_exe_pc_sel
  = Lookup(io.data.exe_br_type, UInt(0, 3),
    Array(   BR_N  -> PC_PLUS4,
      BR_NE -> Mux(!io.data.exe_br_eq,  PC_BRJMP, PC_PLUS4),
      BR_EQ -> Mux( io.data.exe_br_eq,  PC_BRJMP, PC_PLUS4),
      BR_GE -> Mux(!io.data.exe_br_lt,  PC_BRJMP, PC_PLUS4),
      BR_GEU-> Mux(!io.data.exe_br_ltu, PC_BRJMP, PC_PLUS4),
      BR_LT -> Mux( io.data.exe_br_lt,  PC_BRJMP, PC_PLUS4),
      BR_LTU-> Mux( io.data.exe_br_ltu, PC_BRJMP, PC_PLUS4),
      BR_J  -> PC_JADDR,
      BR_JR -> PC_JALR
    ))

  // taken branch || I$ miss
  // val if_kill  = (ctrl_exe_pc_sel != PC_PLUS4) || !io.imem.resp.valid
  val if_kill  = (ctrl_exe_pc_sel != PC_PLUS4) || !io.imem.valid
  // taken branch
  val dec_kill = (ctrl_exe_pc_sel != PC_PLUS4)


  val dec_rs1_addr = io.data.dec_inst(25, 21) // rs TODO: RISCV: decode
  val dec_rs2_addr = io.data.dec_inst(20, 16) // rt TODO:
  val dec_wbaddr = io.data.dec_inst(15, 11)   // rd TODO:
  val dec_rs1_oen = Mux(dec_kill, Bool(false), cs_rs1_oen)
  val dec_rs2_oen = Mux(dec_kill, Bool(false), cs_rs2_oen)

  val exe_reg_wbaddr      = Reg(UInt())
  val mem_reg_wbaddr      = Reg(UInt())
  val wb_reg_wbaddr       = Reg(UInt())
  val exe_reg_ctrl_rf_wen = Reg(init=Bool(false))
  val mem_reg_ctrl_rf_wen = Reg(init=Bool(false))
  val wb_reg_ctrl_rf_wen  = Reg(init=Bool(false))

  // val exe_reg_is_csr = Reg(init=Bool(false))

  // load-use hazard
  val hazard_stall   = Bool()
  // I$ or D$ miss
  val cache_miss_stall = Bool()
  when (!hazard_stall && !cache_miss_stall)
  {
    when (dec_kill)
    {
      exe_reg_wbaddr      := UInt(0)
      exe_reg_ctrl_rf_wen := Bool(false)
      // exe_reg_is_csr      := Bool(false)
    }.otherwise
    {
      exe_reg_wbaddr      := dec_wbaddr
      exe_reg_ctrl_rf_wen := cs_rf_wen
      // exe_reg_is_csr      := cs_csr_cmd != CSR.N
    }
  }.elsewhen (hazard_stall && !cache_miss_stall)
  {
    // kill exe stage
    exe_reg_wbaddr      := UInt(0)
    exe_reg_ctrl_rf_wen := Bool(false)
    // exe_reg_is_csr      := Bool(false)
  }

  mem_reg_wbaddr      := exe_reg_wbaddr
  wb_reg_wbaddr       := mem_reg_wbaddr
  mem_reg_ctrl_rf_wen := exe_reg_ctrl_rf_wen
  wb_reg_ctrl_rf_wen  := mem_reg_ctrl_rf_wen

  val exe_inst_is_load = Reg(init=Bool(false))

  when (!cache_miss_stall)
  {
    exe_inst_is_load := cs_mem_en && (cs_mem_fcn === M_XRD)
  }

  // Stall signal stalls instruction fetch & decode stages,
  // inserts NOP into execute stage,  and drains execute, memory, and writeback stages
  // stalls on I$ misses and on hazards
  if (USE_FULL_BYPASSING)
  {
    // stall for load-use hazard
    hazard_stall :=
      ((exe_inst_is_load) && (exe_reg_wbaddr === dec_rs1_addr) && (exe_reg_wbaddr != UInt(0)) && dec_rs1_oen) ||
      ((exe_inst_is_load) && (exe_reg_wbaddr === dec_rs2_addr) && (exe_reg_wbaddr != UInt(0)) && dec_rs2_oen) // || ((exe_reg_is_csr))
  }
  else
  {
    // stall for all hazards
    hazard_stall :=
      ((exe_reg_wbaddr === dec_rs1_addr) && (dec_rs1_addr != UInt(0)) && exe_reg_ctrl_rf_wen && dec_rs1_oen) ||
      ((mem_reg_wbaddr === dec_rs1_addr) && (dec_rs1_addr != UInt(0)) && mem_reg_ctrl_rf_wen && dec_rs1_oen) ||
      ((wb_reg_wbaddr  === dec_rs1_addr) && (dec_rs1_addr != UInt(0)) &&  wb_reg_ctrl_rf_wen && dec_rs1_oen) ||
      ((exe_reg_wbaddr === dec_rs2_addr) && (dec_rs2_addr != UInt(0)) && exe_reg_ctrl_rf_wen && dec_rs2_oen) ||
      ((mem_reg_wbaddr === dec_rs2_addr) && (dec_rs2_addr != UInt(0)) && mem_reg_ctrl_rf_wen && dec_rs2_oen) ||
      ((wb_reg_wbaddr  === dec_rs2_addr) && (dec_rs2_addr != UInt(0)) &&  wb_reg_ctrl_rf_wen && dec_rs2_oen) ||
      ((exe_inst_is_load) && (exe_reg_wbaddr === dec_rs1_addr) && (exe_reg_wbaddr != UInt(0)) && dec_rs1_oen) ||
      ((exe_inst_is_load) && (exe_reg_wbaddr === dec_rs2_addr) && (exe_reg_wbaddr != UInt(0)) && dec_rs2_oen) // || ((exe_reg_is_csr))
  }

  val dmem_val   = io.data.mem_ctrl_dmem_val
  // stall full pipeline on I$ miss || D$ miss
  // TODO: MEMIFCE:
  // cache_miss_stall := !io.imem.resp.valid || !((dmem_val && io.dmem.resp.valid) || !dmem_val)
  cache_miss_stall := !io.imem.valid || !((dmem_val && io.dmem.valid) || !dmem_val)


  io.ctrl.dec_stall  := hazard_stall     // stall if, dec stage (pipeline hazard)
  io.ctrl.full_stall := cache_miss_stall // stall entire pipeline (I/D$ miss)
  io.ctrl.exe_pc_sel := ctrl_exe_pc_sel
  io.ctrl.br_type    := cs_br_type
  io.ctrl.if_kill    := if_kill          // full_stall
  io.ctrl.dec_kill   := dec_kill
  io.ctrl.op1_sel    := cs_op1_sel
  io.ctrl.op2_sel    := cs_op2_sel
  io.ctrl.alu_fun    := cs_alu_fun
  io.ctrl.wb_sel     := cs_wb_sel
  io.ctrl.rf_wen     := cs_rf_wen
  // io.ctl.csr_cmd    := cs_csr_cmd

  // TODO: MEMIFCE:
  io.imem.ren        := Bool(true)
  io.imem.wen        := Bool(false)
  io.ctrl.mem_val    := cs_mem_en
  io.ctrl.mem_fcn    := cs_mem_fcn
  /*io.imem.req.valid := Bool(true)
  io.imem.req.bits.fcn := M_XRD
  io.imem.req.bits.typ := MT_WU
  io.ctrl.mem_fcn    := cs_mem_fcn
  io.ctrl.mem_typ   := cs_msk_sel*/
}
