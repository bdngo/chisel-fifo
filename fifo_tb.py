import cocotb
from cocotb.clock import Clock
from cocotb.triggers import RisingEdge, ClockCycles, FallingEdge

@cocotb.test()
async def test_basic(dut):
    clock = Clock(dut.clk, 10, units="ns")  # Create a 10us period clock on port clk
    cocotb.start_soon(clock.start())  # Start the clock

    """
    fork
        begin
            forever begin
                clk = !clk;
                #10;
            end
        end
    join_none
    """

    dut.rst.value = 1
    await ClockCycles(dut.clk, 1)
    dut.rst.value = 0
    await FallingEdge(dut.clk)

    assert dut.empty.value and not dut.full.value

    await RisingEdge(dut.clk)

    test_values = [i + 10 for i in range(32)]
    dut.wr_en.value = True
    for i in range(31):
        dut.din.value = test_values[i]
        assert not dut.empty.value and not dut.full.value
        await RisingEdge(dut.clk)

    dut.din.value = test_values[-1]
    assert not dut.empty.value and dut.full.value

    await ClockCycles(dut.clk, 10)
    assert not dut.empty.value and dut.full.value

    dut.wr_en.value = True
    for _ in range(20):
        dut.din.value = 5
        assert not dut.empty.value and dut.full.value
    dut.wr_en = False

    await ClockCycles(dut.clk, 5)
    received_values = []
    dut.rd_en.value = True
    for i in range(31):
        received_values.append(dut.dout.value)
        assert not dut.empty.value and not dut.full.value
        await RisingEdge(dut.clk)

    received_values.append(dut.dout.value)
    assert dut.empty.value and not dut.full.value

    await ClockCycles(dut.clk, 10)
    assert dut.empty.value and not dut.full.value

    assert test_values == received_values

    received_values[0] = dut.dout.value
    assert dut.empty.value and not dut.full.value
