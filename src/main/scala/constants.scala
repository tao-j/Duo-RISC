package Common

import Chisel._

object MIPSInstructions {
  def ADD_                = Bits("b000000????????????????????100000")
  def SUB_                = Bits("b000000????????????????????100010")
  def AND_                = Bits("b000000????????????????????100100")
  def OR_                 = Bits("b000000????????????????????100101")
  def XOR_                = Bits("b000000????????????????????100110")
  def SLT_                = Bits("b000000????????????????????101010")
  def SLL_                = Bits("b000000????????????????????000000")
  def SRL_                = Bits("b000000????????????????????000010")
  def SRA_                = Bits("b000000????????????????????000011")

  def JR_                 = Bits("b000000????????????????????001000")
  def JALR_               = Bits("b000000????????????????????001001")

  def ADDI_               = Bits("b001000??????????????????????????")
  def ANDI_               = Bits("b001100??????????????????????????")
  def ORI_                = Bits("b001101??????????????????????????")
  def XORI_               = Bits("b001110??????????????????????????")

  def LW_                 = Bits("b100011??????????????????????????")
  def SW_                 = Bits("b101011??????????????????????????")

  def BEQ_                = Bits("b000100??????????????????????????")
  def BNE_                = Bits("b000101??????????????????????????")

  def LUI_                = Bits("b001111??????????????????????????")
  def J_                  = Bits("b000010??????????????????????????")
  def JAL_                = Bits("b000011??????????????????????????")

  def NOP_                = Bits("b00000000000000000000000000000000")
}


trait AddressConstants {
  val START_ADDR = UInt(0, 32)
}

trait ScalarOperationConstants {

  // Control Signals
  val Y        = Bool(true)
  val N        = Bool(false)

  // PC Select Signal
  val PC_PLUS4 = UInt(0, 3)  // PC + 4
  val PC_BRJMP = UInt(1, 3)  // brjmp_target
  val PC_JALR  = UInt(2, 3)  // jump_reg_target
  val PC_JADDR = UInt(3, 3)  // MIPS unconditioned jump

  // Branch Type
  val BR_N     = UInt(0, 4)  // Next
  val BR_NE    = UInt(1, 4)  // Branch on NotEqual
  val BR_EQ    = UInt(2, 4)  // Branch on Equal
  val BR_GE    = UInt(3, 4)  // Branch on Greater/Equal
  val BR_GEU   = UInt(4, 4)  // Branch on Greater/Equal Unsigned
  val BR_LT    = UInt(5, 4)  // Branch on Less Than
  val BR_LTU   = UInt(6, 4)  // Branch on Less Than Unsigned
  val BR_J     = UInt(7, 4)  // Jump
  val BR_JR    = UInt(8, 4)  // Jump Register

  // RS1 Operand Select Signal
  val OP1_RS1   = UInt(0, 2) // Register Source #1
  val OP1_PC    = UInt(1, 2) // PC
  val OP1_IMZ   = UInt(2, 2) // Zero-extended Immediate from RS1 field, for use by CSRI instructions
  val OP1_X     = UInt(0, 2)

  // RS2 Operand Select Signal
  val OP2_RS2    = UInt(0, 4) // Register Source #2
  val OP2_ITYPE  = UInt(1, 4) // immediate, I-type
  val OP2_STYPE  = UInt(2, 4) // immediate, S-type
  val OP2_SBTYPE = UInt(3, 4) // immediate, B
  val OP2_UTYPE  = UInt(4, 4) // immediate, U-type
  val OP2_UJTYPE = UInt(5, 4) // immediate, J-type
  val OP2_SEXT   = UInt(6, 4) // immediate, MIPS sign extend
  val OP2_ZEXT   = UInt(7, 4) // immediate, MIPS zero extend
  val OP2_HIGH   = UInt(8, 4) // immediate, MIPS high extend
  val OP2_ADDR   = UInt(9, 4) // immediate, MIPS absolute address
  val OP2_X      = UInt(0, 4)

  // Register Operand Output Enable Signal
  val OEN_0   = Bool(false)
  val OEN_1   = Bool(true)

  // Register File Write Enable Signal
  val REN_0   = Bool(false)
  val REN_1   = Bool(true)

  // ALU Operation Signal
  val ALU_ADD    = UInt ( 0, 4)
  val ALU_SUB    = UInt ( 1, 4)
  val ALU_SLL    = UInt ( 2, 4)
  val ALU_SRL    = UInt ( 3, 4)
  val ALU_SRA    = UInt ( 4, 4)
  val ALU_AND    = UInt ( 5, 4)
  val ALU_OR     = UInt ( 6, 4)
  val ALU_XOR    = UInt ( 7, 4)
  val ALU_SLT    = UInt ( 8, 4)
  val ALU_SLTU   = UInt ( 9, 4)
  val ALU_COPY_1 = UInt (10, 4)
  val ALU_COPY_2 = UInt (11, 4)
  val ALU_SLLI   = UInt (12, 4) // shamt field for MIPS
  val ALU_SRAI   = UInt (13, 4) // shamt field for MIPS
  val ALU_SRLI   = UInt (14, 4) // shamt field for MIPS
  val ALU_X      = UInt ( 0, 4)

  // Writeback Select Signal
  val WB_ALU  = UInt(0, 2)
  val WB_MEM  = UInt(1, 2)
  val WB_PC4  = UInt(2, 2)
  val WB_P4R  = UInt(3, 2)
  // val WB_CSR  = UInt(3, 2)
  val WB_X    = UInt(0, 2)

  // Memory Write Signal
  val MWR_0   = Bool(false)
  val MWR_1   = Bool(true)
  val MWR_X   = Bool(false)

  // Memory Enable Signal
  val MEN_0   = Bool(false)
  val MEN_1   = Bool(true)
  val MEN_X   = Bool(false)

  // Memory Mask Type Signal
  val MSK_B   = UInt(0, 3)
  val MSK_BU  = UInt(1, 3)
  val MSK_H   = UInt(2, 3)
  val MSK_HU  = UInt(3, 3)
  val MSK_W   = UInt(4, 3)
  val MSK_X   = UInt(4, 3)
}

trait MemoryOperationConstants
{
  val MT_X  = Bits(0, 3)
  val MT_B  = Bits(1, 3)
  val MT_H  = Bits(2, 3)
  val MT_W  = Bits(3, 3)
  val MT_D  = Bits(4, 3)
  val MT_BU = Bits(5, 3)
  val MT_HU = Bits(6, 3)
  val MT_WU = Bits(7, 3)

  val M_X   = Bits("b0", 1)
  val M_XRD = Bits("b0", 1) // int load
  val M_XWR = Bits("b1", 1) // int store
}

object Constants extends
ScalarOperationConstants with
MemoryOperationConstants with
AddressConstants
{

  // TODO: turn off full bypassing is not tested under MIPS
  val USE_FULL_BYPASSING = true

  // TODO: question, when will compiler generate NOPs?
  // The Bubble Instruction (Machine generated NOP)
  // Insert (XOR x0,x0,x0) which is different from software compiler
  // generated NOPs which are (ADDI x0, x0, 0).
  // Reasoning for this is to let visualizers and stat-trackers differentiate
  // between software NOPs and machine-generated Bubbles in the pipeline.
  val BUBBLE  = Bits(0x4033, 32)
}
