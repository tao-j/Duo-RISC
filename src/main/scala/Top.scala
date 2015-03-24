import Chisel._

class Top extends Module {
  val io = new Bundle {
    val peripheral = new PeripheralIO
  }

  val core = Module(new Core)
  val memory = Module(new Memory)

  core.io.mem <> memory.io

  val peripheral = Module(new Peripheral)
  io.peripheral <> peripheral.io.pin_hub
  peripheral.io.raw_hub.segment_display_raw :=  memory.io.debug(15,0)


  peripheral.io.raw_hub.analog_monitor_raw.color :=
    Mux (peripheral.io.raw_hub.analog_monitor_raw.video_enable,
      peripheral.io.raw_hub.analog_monitor_raw.x_coordinate +
        peripheral.io.raw_hub.analog_monitor_raw.y_coordinate,
      UInt(0))
}

class TopTests(c: Top) extends Tester(c) {

//  val v1 = rnd.nextInt(1 << 8)
//  val v2 = rnd.nextInt(1 << 8)
//  val v3 = rnd.nextInt(1 << 8)
//  val v4 = rnd.nextInt(1 << 8)
//  pokeAt(c.memory.textSeg, (v1 + v2 + v3 + v4) % (1 << 31), 0)
//  pokeAt(c.memory.textSeg, v1, 0x80 >> 2)
//  pokeAt(c.memory.textSeg, v2, 0x84 >> 2)
//  pokeAt(c.memory.textSeg, v3, 0x88 >> 2)
//  pokeAt(c.memory.textSeg, v4, 0x8c >> 2)


  reset(1)
  step (200000)
  //TODO: val prv_pc = peek(c.core.pc)

  peekAt(c.memory.textSeg, 0x90 >> 2)
  peek(c.memory.io.debug)
  peek(c.peripheral.io.pin_hub.segment_display_pin.segment)

  //see

  def see: Unit = {
    //peek()
    //peekAt()
  }
}

object Top {
  def main(args: Array[String]): Unit = {
    val tutArgs = args.slice(1, args.length)
    args.foreach(arg => println(arg))
    chiselMainTest(tutArgs, () => Module(new Top())) {
      c => new TopTests(c) }
  }
}

/*
module multiple_cycle_cpu (cpu_clk, resetn, m_addr, d_f_mem, d_t_mem, w_d_mem, wr_vram, rd_vram, io_rdn, state);
    input  cpu_clk, resetn; // cpu clock and resetn (active low)
    output [31:0] m_addr;   // memory address
    input  [31:0] d_f_mem;  // data to memory
    output [31:0] d_t_mem;  // data from memory
    output w_d_mem;         // write data memory
    output wr_vram;         // write video memory, for vga
    output rd_vram;         // vram read
    output io_rdn;          // i/o read (active low), for ps2 keyboard
    output [2:0] state;     // state for test

    parameter [2:0] s_if  = 3'b000, // IF  state
                    s_id  = 3'b001, // ID  state
                    s_exe = 3'b010, // EXE state
                    s_mem = 3'b011, // MEM state
                    s_wb  = 3'b100; // WB  state

    // registers
    reg [31:0] pc = 0;      // program counter
    reg [31:0] ir = 0;      // instruction register
    reg [31:0] c  = 0;      // register C
    reg [31:0] d  = 0;      // register D
    reg [2:0] state = s_if; // state register

    // instruction format
    wire [05:00] opcode = ir[31:26];
    wire [04:00] rs     = ir[25:21];
    wire [04:00] rt     = ir[20:16];
    wire [04:00] rd     = ir[15:11];
    wire [04:00] sa     = ir[10:06];
    wire [05:00] func   = ir[05:00];
    wire [15:00] imm    = ir[15:00];
    wire [25:00] addr   = ir[25:00];
    wire         sign   = ir[15];
    wire [31:00] s_imm  = {{16{sign}},imm}; // sign-ext. imm
    wire [31:00] z_imm  = {16'h0000,imm};   // zero-ext. imm

    // for next_pc
    wire [31:00] p4_addr = pc + 32'h4;                 // pc+4
    wire [31:00] br_addr = pc + {s_imm[29:0],2'b00};   // beq, bne
    wire [31:00] jr_addr = a;                          // jr
    wire [31:00] jp_addr = {pc[31:28],addr,2'b00};     // j, jal

    // instruction decode
    wire i_add  = (opcode == 6'h00) & (func == 6'h20); // add
    wire i_sub  = (opcode == 6'h00) & (func == 6'h22); // sub
    wire i_and  = (opcode == 6'h00) & (func == 6'h24); // and
    wire i_or   = (opcode == 6'h00) & (func == 6'h25); // or
    wire i_xor  = (opcode == 6'h00) & (func == 6'h26); // xor
    wire i_sll  = (opcode == 6'h00) & (func == 6'h00); // all
    wire i_srl  = (opcode == 6'h00) & (func == 6'h02); // srl
    wire i_sra  = (opcode == 6'h00) & (func == 6'h03); // sra
    wire i_jr   = (opcode == 6'h00) & (func == 6'h08); // jr
    wire i_addi = (opcode == 6'h08);                   // addi
    wire i_andi = (opcode == 6'h0c);                   // andi
    wire i_ori  = (opcode == 6'h0d);                   // ori
    wire i_xori = (opcode == 6'h0e);                   // xori
    wire i_lw   = (opcode == 6'h23);                   // lw
    wire i_sw   = (opcode == 6'h2b);                   // sw
    wire i_beq  = (opcode == 6'h04);                   // beq
    wire i_bne  = (opcode == 6'h05);                   // bne
    wire i_lui  = (opcode == 6'h0f);                   // lui
    wire i_j    = (opcode == 6'h02);                   // j
    wire i_jal  = (opcode == 6'h03);                   // jal

    // program counter
    always @ (posedge cpu_clk or negedge resetn) begin
        if (!resetn)  pc <= 0;
        else if (wpc) pc <= next_pc; // wpc
    end

    // instruction register
    always @ (posedge cpu_clk or negedge resetn) begin
        if (!resetn)  ir <= 0;
        else if (wir) ir <= d_f_mem; // wir
    end

    // register C
    always @ (posedge cpu_clk or negedge resetn) begin
        if (!resetn)  c <= 0;
        else          c <= c_in;
    end

    // register D
    always @ (posedge cpu_clk or negedge resetn) begin
        if (!resetn)  d <= 0;
        else          d <= d_f_mem;
    end

    // state register
    always @ (posedge cpu_clk or negedge resetn) begin
        if (!resetn)  state <= s_if;
        else          state <= next_state;
    end

    // register file
    reg  [31:0] regfile [31:1]; // $1 - $31 regs
    wire [31:0] a = (rs==0) ? 0 : regfile[rs]; // read port 0
    wire [31:0] b = (rt==0) ? 0 : regfile[rt]; // read port 1
    always @ (posedge cpu_clk or negedge resetn) begin
        if (resetn == 0) begin  // reset
            integer i;
            for (i = 1; i < 32; i = i + 1)
                regfile[i] <= 0;
        end else begin                         // write port
            if (wreg && (dest_rn != 0)) begin  // wreg
                regfile[dest_rn] <= data_2_rf;
            end
        end
    end

    // memory-mapped I/O
    wire io_space = c[31] & // i/o space:
                   ~c[30] & // a000_0000 -
                    c[29];  // bfff_ffff

    // vram space, can be done outside of cpu
    wire vr_space = c[31] & // vram space:
                    c[30] & // c000_0000 -
                   ~c[29];  // dfff_ffff

    // output signals
    assign w_d_mem =   s_mem & i_lw & ~io_space & ~vr_space; // write data memory
    assign d_t_mem =   b;                                    // write reg[rt] to memory
    assign io_rdn  = ~(s_mem & i_lw &  io_space);            // i/o read (active low)
    assign wr_vram =   s_mem & i_sw &  vr_space;             // write  video ram
    assign rd_vram =   s_mem & i_lw &  vr_space;             // read   video ram

    // will be nets (wires):
    reg        wpc;           // 0. write PC
    reg        wir;           // 1. write IR
    reg        wreg;          // 2. write register file
    reg        wmem;          // 3. write memory
    reg [31:0] next_pc;       // 4. next state
    reg [31:0] c_in;          // 5. to register C
    reg [31:0] data_2_rf;     // 6. select ALU output
    reg  [4:0] dest_rn;       // 7. destination register
    reg  [2:0] next_state;    // 8. next state
    reg [31:0] m_addr;        // 9. memory address

    // combinational circuit:
    always @* begin           //    default settings:
        wpc        = 0;       // 0. do not write PC
        wir        = 0;       // 1. do not write IR
        wreg       = 0;       // 2. do not write register file
        wmem       = 0;       // 3. do not write data memory
        next_pc    = p4_addr; // 4. next pc = pc + 4
        c_in       = 0;       // 5. for register C input
        data_2_rf  = c;       // 6. select ALU output
        dest_rn    = rd;      // 7. destinarion reg = rd
        next_state = s_if;    // 8. next state = IF
        m_addr     = pc;      // 9. memory address = PC

        case (state) //---------------------------------------- IF:
            s_if: begin             // IF state
                wpc        = 1;     // write PC
                wir        = 1;     // write IR
                next_state = s_id;  // next state: ID
            end //--------------------------------------------- ID:
            s_id: begin                   // ID state
                if (i_j) begin            // j   -> IF
                    wpc        = 1;       // write PC
                    next_pc    = jp_addr; // jump address
                    next_state = s_if;    // next state: IF
                end else if (i_jal) begin // jal -> IF
                    wpc        = 1;       // write PC
                    next_pc    = jp_addr; // jump address
                    wreg       = 1;       // save return address
                    dest_rn    = 5'd31;   // to $31
                    data_2_rf  = pc;      // return address
                    next_state = s_if;    // next state: IF
                end else if (i_jr) begin  // jr  -> IF
                    wpc        = 1;       // write PC
                    next_pc    = jr_addr; // jump address
                    next_state = s_if;    // next state: IF
                end else begin            // other instructions
                    c_in = br_addr;       // branch address
                    next_state = s_exe;   // others -> EXE
                end
            end //--------------------------------------------- EXE:
            s_exe: begin                  // EXE state
            case (1'b1)
                i_add: begin              // add  -> WB
                    c_in = a + b;
                    next_state = s_wb; end
                i_sub: begin              // sub  -> WB
                    c_in = a - b;
                    next_state = s_wb; end
                i_and: begin              // and  -> WB
                    c_in = a & b;
                    next_state = s_wb; end
                i_or: begin               // or   -> WB
                    c_in = a | b;
                    next_state = s_wb; end
                i_xor: begin              // xor  -> WB
                    c_in = a ^ b;
                    next_state = s_wb; end
                i_sll: begin              // sll  -> WB
                    c_in = b << sa;
                    next_state = s_wb; end
                i_srl: begin              // srl  -> WB
                    c_in = b >> sa;
                    next_state = s_wb; end
                i_sra: begin              // sra  -> WB
                    c_in = $signed(b) >>> sa;
                    next_state = s_wb; end
                i_addi: begin             // addi -> WB
                    c_in = a + s_imm;
                    next_state = s_wb; end
                i_andi: begin             // andi -> WB
                    c_in = a & z_imm;
                    next_state = s_wb; end
                i_ori: begin              // ori  -> WB
                    c_in = a | z_imm;
                    next_state = s_wb; end
                i_xori: begin             // xori -> WB
                    c_in = a ^ z_imm;
                    next_state = s_wb; end
                i_lw: begin               // lw  -> MEM
                    c_in = a + s_imm;
                    next_state = s_mem; end
                i_sw: begin               // sw  -> MEM
                    c_in = a + s_imm;
                    next_state = s_mem; end
                i_beq: begin              // beq  -> IF
                    if (a == b) begin
                        next_pc = br_addr;
                        wpc = 1;
                    end
                    next_state = s_if; end
                i_bne: begin              // bne  -> IF
                    if (a != b) begin
                        next_pc = br_addr;
                        wpc = 1;
                    end
                    next_state = s_if; end
                i_lui: begin              // lui  -> WB
                    c_in = {imm,16'h0};
                    next_state = s_wb; end
                default: next_state = s_if;
            endcase
            end //--------------------------------------------- MEM:
            s_mem: begin                  // MEM state
                m_addr = c;               // data address
                if (i_lw) begin           // lw -> WB
                    next_state = s_wb;    // next state: WB
                end else begin            // sw -> IF
                    wmem = 1;             // write memory
                    next_state = s_if;    // next state: IF
                end
            end //--------------------------------------------- WB:
            s_wb: begin                   // WB state
                if (i_lw) data_2_rf = d;  // select memory data
                if (i_lw || i_addi || i_andi || i_ori || i_xori || i_lui)
                    dest_rn = rt;         // destinarion reg: rt
                wreg = 1;                 // write register file
                next_state = s_if;        // next state: IF
            end //--------------------------------------------- END

            default: begin
                next_state = s_if;        // default state
            end
        endcase
    end
endmodule

 */