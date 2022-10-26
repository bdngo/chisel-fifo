module fifo #(
    parameter WIDTH = 8,
    parameter DEPTH = 32,
    parameter POINTER_WIDTH = $clog2(DEPTH)
) (
    input clk, rst,

    // Write side
    input wr_en,
    input [WIDTH-1:0] din,
    output full,

    // Read side
    input rd_en,
    output [WIDTH-1:0] dout,
    output empty
);
    // assign full = 1'b1;
    // assign empty = 1'b0;
    // assign dout = 0;

    reg [WIDTH-1:0] buffer [DEPTH-1:0];

    reg [POINTER_WIDTH-1:0] rd_ptr;
    reg [POINTER_WIDTH-1:0] wr_ptr;

    reg rd_wrap;
    reg wr_wrap;

    reg [WIDTH-1:0] dout_reg;

    always @(posedge clk) begin
        if (rst) begin
            rd_ptr <= 0;
            wr_ptr <= 0;

            rd_wrap <= 0;
            wr_wrap <= 0;
        end else begin
            if (wr_en && !full) begin
                /* verilator lint_off WIDTH */
                if (wr_ptr == DEPTH - 1) begin
                    wr_wrap <= ~wr_wrap;
                end
                wr_ptr <= wr_ptr + 1;
                buffer[wr_ptr] <= din;
            end
            if (rd_en & !empty) begin
                /* verilator lint_off WIDTH */
                if (rd_ptr == DEPTH - 1) begin
                    rd_wrap <= ~rd_wrap;
                end
                rd_ptr <= rd_ptr + 1;
                dout_reg <= buffer[rd_ptr];
            end
        end
    end

    assign empty = (rd_ptr == wr_ptr) && (rd_wrap == wr_wrap);
    assign full = (rd_ptr == wr_ptr) && (rd_wrap != wr_wrap);
    assign dout = dout_reg;
endmodule
