import Chisel._

class MemoryIO() extends Bundle
{
  // TODO: valid wrapper
  val d_f_mem = UInt(INPUT, 32).flip()   // data from memory
  val r_m_addr  = UInt(OUTPUT, 32).flip()  // read memory address
  val w_m_addr  = UInt(OUTPUT, 32).flip()  // write memory address
  val d_t_mem = UInt(OUTPUT, 32).flip()  // data to memory
  val w_d_mem = Bool(OUTPUT).flip()  // write data memory
  val debug = UInt(OUTPUT, 32)
}

class Memory() extends Module {
  val io = new MemoryIO()

  val textSeg = Mem(UInt(width = 32), 38, true)
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
  val prog1 = Array(
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
    UInt("h03e00008"),
    UInt("h00000bee"),
    UInt("h00000bed"),
    UInt("h00000add"),
    UInt("h00000fee"),
    UInt("h_dead_beef")
  )
  /*
  test program 2 .text at 0x00003000 .data at 0x00000000
 Address    Code        Basic                     Source

0x00003000  0x20010001  addi $1,$0,0x00000001 1          addi $1,$0,1
0x00003004  0xac010000  sw $1,0x00000000($0)  2          sw   $1,0
0x00003008  0x00010880  sll $1,$1,0x00000002  3          sll  $1,$1,2
0x0000300c  0xac010004  sw $1,0x00000004($0)  4          sw   $1,4
0x00003010  0x00010880  sll $1,$1,0x00000002  5          sll  $1,$1,2
0x00003014  0xac010008  sw $1,0x00000008($0)  6          sw   $1,8
0x00003018  0x00000022  sub $0,$0,$0          7          sub  $0,$0,$0     # set reg[0] to 0, use as base
0x0000301c  0x8c010000  lw $1,0x00000000($0)  8          lw   $1,0($0)     # reg[1] <- mem[0] (= 1)
0x00003020  0x8c020004  lw $2,0x00000004($0)  9          lw   $2,4($0)     # reg[2] <- mem[4] (= A)
0x00003024  0x8c030008  lw $3,0x00000008($0)  10         lw   $3,8($0)     # reg[3] <- mem[8] (= B)
0x00003028  0x00842022  sub $4,$4,$4          11         sub  $4,$4,$4     # reg[4] <- 0, running total
0x0000302c  0x00442020  add $4,$2,$4          12   loop: add  $4,$2,$4     # reg[4]+ = A
0x00003030  0x0043282a  slt $5,$2,$3          13         slt  $5,$2,$3     # reg[5] <- A < B
0x00003034  0x10a00002  beq $5,$0,0x00000002  14         beq  $5,$0,end    # if reg[5] = FALSE, go forward 2 instructions
0x00003038  0x00221020  add $2,$1,$2          15         add  $2,$1,$2     # A++
0x0000303c  0x1000fffb  beq $0,$0,0xfffffffb  16         beq  $0,$0,loop   # go back 5 instructions
0x00003040  0xac040000  sw $4,0x00000000($0)  17   end:  sw   $4,0($0)     # mem[0] <- reg[4]
0x00003044  0x1000ffff  beq $0,$0,0xffffffff  18   dead: beq  $0,$0,dead   # program is over, keep looping back to here
   */
  val prog2 = Array(
    UInt("h20010001"),
    UInt("hac010000"),
    UInt("h00010880"),
    UInt("hac010004"),
    UInt("h00010880"),
    UInt("hac010008"),
    UInt("h00000022"),
    UInt("h8c010000"),
    UInt("h8c020004"),
    UInt("h8c030008"),
    UInt("h00842022"),
    UInt("h00442020"),
    UInt("h0043282a"),
    UInt("h10a00002"),
    UInt("h00221020"),
    UInt("h1000fffb"),
    UInt("hac040000"),
    UInt("h1000ffff"),
  UInt("h_dead_beef")
  )

  val prog = prog1

  when (this.reset) {
    for (i <- 0 until prog.length) {
      textSeg(i) := prog(i)
    }
  }

  val dout = UInt()
  dout := UInt("h_dead_beef")
  val r_word_addr = io.r_m_addr >> UInt(2)
  val w_word_addr = io.w_m_addr >> UInt(2)


  when (io.w_d_mem) { textSeg(w_word_addr) := io.d_t_mem }
  .otherwise {dout := textSeg(r_word_addr) }

  io.d_f_mem := dout

  io.debug := textSeg(0x90 >> 2)
}