import Chisel._

// TODO: remove debug port
trait Debug {
  val debug = UInt(OUTPUT, 32)
}

class CachePort extends Bundle with Debug{
  val addr = UInt(INPUT, 32)
  val data_i = UInt(INPUT, 32)
  val data_o = UInt(OUTPUT, 32)
  val valid  = Bool(OUTPUT)
  val wen    = Bool(INPUT)
  val ren    = Bool(INPUT)
}

abstract class CacheBase extends Module {
  val io = new CachePort

  val mem = Mem(UInt(width = 32), 32, true)

  val dout = UInt()
  dout := UInt("h_a_dead_bee")
  val r_word_addr = io.addr >> UInt(2)
  val w_word_addr = io.addr >> UInt(2)


  when (io.wen) { mem(w_word_addr) := io.data_i }
  when (io.ren) { dout := mem(r_word_addr) }

  io.data_o := dout

  // TODO: this is a temporary solution
  io.valid := Bool(true)
}

class ICache extends CacheBase {
  /*
test program 1 .text at 0x00003000 .data at 0x00000000
0  : 3c010000; % (00) main:  lui  r1, 0          # address of data[0]     %
1  : 34240080; % (04)        ori  r4, r1, 0x80   # address of data[0]     %
2  : 20050004; % (08)        addi r5, r0, 4      # counter                %
3  : 0c000018; % (0c) call:  jal  sum            # call function          %
4  : ac820000; % (10)        sw   r2, 0(r4)      # store result           %
5  : 8c890000; % (14)        lw   r9, 0(r4)      # check sw               %
6  : 01244022; % (18)        sub  r8, r9, r4     # sub: r8 <-- r9 - r4    %
7  : 20050003; % (1c)        addi r5, r0, 3      # counter                %
8  : 20a5ffff; % (20) loop2: addi r5, r5, -1     # counter - 1            %
9  : 34a8ffff; % (24)        ori  r8, r5, 0xffff # zero-extend: 0000ffff  %
A  : 39085555; % (28)        xori r8, r8, 0x5555 # zero-extend: 0000aaaa  %
B  : 2009ffff; % (2c)        addi r9, r0, -1     # sign-extend: ffffffff  %
C  : 312affff; % (30)        andi r10, r9,0xffff # zero-extend: 0000ffff  %
D  : 01493025; % (34)        or   r6, r10, r9    # or: ffffffff           %
E  : 01494026; % (38)        xor  r8, r10, r9    # xor: ffff0000          %
F  : 01463824; % (3c)        and  r7, r10, r6    # and: 0000ffff          %
10 : 10a00001; % (40)        beq  r5, r0, shift  # if r5 = 0, goto shift  %
11 : 08000008; % (44)        j    loop2          # jump loop2             %
12 : 2005ffff; % (48) shift: addi r5, r0, -1     # r5 = ffffffff          %
13 : 000543c0; % (4c)        sll  r8, r5, 15     # <<15 = ffff8000        %
14 : 00084400; % (50)        sll  r8, r8, 16     # <<16 = 80000000        %
15 : 00084403; % (54)        sra  r8, r8, 16     # >>16 = ffff8000(arith) %
16 : 000843c2; % (58)        srl  r8, r8, 15     # >>15 = 0001ffff(logic) %
17 : 08000017; % (5c) finish: j   finish         # dead loop              %
18 : 00004020; % (60) sum:   add  r8, r0, r0     # sum                    %
19 : 8c890000; % (64) loop:  lw   r9, 0(r4)      # load data              %
1A : 20840004; % (68)        addi r4, r4, 4      # address + 4            %
1B : 01094020; % (6c)        add  r8, r8, r9     # sum                    %
1C : 20a5ffff; % (70)        addi r5, r5, -1     # counter - 1            %
1D : 14a0fffb; % (74)        bne  r5, r0, loop   # finish?                %
1E : 00081000; % (78)        sll  r2, r8, 0      # move result to v0      %
1F : 03e00008; % (7c)        jr   r31            # return                 %
 */
  // TODO: replace binary with proper .text .data address, corresponding to START_ADDR
  val prog = Array(
    UInt("h3c010000"),
    UInt("h34240080"),
    UInt("h20050004"),
    UInt("h0c000c18"),
    UInt("hac820000"),
    UInt("h8c890000"),
    UInt("h01244022"),
    UInt("h20050003"),
    UInt("h20a5ffff"),
    UInt("h34a8ffff"),
    UInt("h39085555"),
    UInt("h2009ffff"),
    UInt("h312affff"),
    UInt("h01493025"),
    UInt("h01494026"),
    UInt("h01463824"),
    UInt("h10a00001"),
    UInt("h08000c08"),
    UInt("h2005ffff"),
    UInt("h000543c0"),
    UInt("h00084400"),
    UInt("h00084403"),
    UInt("h000843c2"),
    UInt("h08000c17"),
    UInt("h00004020"),
    UInt("h8c890000"),
    UInt("h20840004"),
    UInt("h01094020"),
    UInt("h20a5ffff"),
    UInt("h14a0fffb"),
    UInt("h00081000"),
    UInt("h03e00008")
  )
  assert(Bool(prog.length <= mem.length), "Not enough Imem")
  when (this.reset) {
    for (i <- 0 until prog.length) {
      mem(i) := prog(i)
    }
  }
}

class DCache extends CacheBase {
  val data = Array(
  UInt("h00000bee"),
  UInt("h00000bed"),
  UInt("h00000add"),
  UInt("h00000fee"),
  UInt("h_dead_beef")
  )
  when (this.reset) {
    mem(0) := data(0)
    mem(1) := data(1)
    mem(2) := data(2)
    mem(3) := data(3)
    mem(4) := data(4)
  }
  io.debug := mem(4)
}
