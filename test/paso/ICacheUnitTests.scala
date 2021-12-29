package paso

import chipsalliance.rocketchip.config._
import chiseltest._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._
import freechips.rocketchip.tilelink._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chisel3.util._
import tilelink.{MonitorDirection, PasoTLMonitor}
import verif.{DefaultTLParams, VerifTestUtils}

class ICacheStandalone(
  cacheParams:           ICacheParams,
  params:                Parameters,
  val withTLMonitor:     Boolean = false,
  val allowCorruption:   Boolean = true,
  val printTransactions: Boolean = false)
    extends LazyModule()(params) {
  val cache = LazyModule(new ICache(cacheParams, 0)(p))
  val ioOutNode = BundleBridgeSink[TLBundle]()
  val bridge = TLToBundleBridge(DefaultTLParams.manager)
  ioOutNode := bridge
  bridge := TLBuffer() := cache.masterNode
  lazy val module = new ICacheStandaloneImpl(this)
}

class ICacheStandaloneImpl(w: ICacheStandalone) extends LazyModuleImp(w) {
  // TileLink I/O
  val tl = IO(chiselTypeOf(w.ioOutNode.bundle))
  tl <> w.ioOutNode.bundle

  if (!w.allowCorruption) {
    assume(!tl.d.bits.corrupt, "d channel response is never corrupted")
  }

  if (w.withTLMonitor) {
    // TileLink Monitor
    val (_, edge) = w.bridge.in.head
    val monitor = Module(new PasoTLMonitor(edge, MonitorDirection.Receiver))
    monitor.io.in := tl
  }

  // other (frontend) I/O
  val io = IO(chiselTypeOf(w.cache.module.io))
  io <> w.cache.module.io

  // print transactions on the I/Os
  if (w.printTransactions) {
    val cycleCount = RegInit(0.U(32.W))
    cycleCount := cycleCount + 1.U

    val anyFire = Seq(io.req.fire, io.resp.fire, tl.a.fire, tl.d.fire).reduce(_ || _)
    when(anyFire) {
      printf(p"[${cycleCount}] --------------------------------\n")
      when(io.req.fire) {
        printf(p"io.req: addr=${Hexadecimal(io.req.bits.addr)}\n")
      }
      when(io.resp.fire) {
        printf(p"io.resp: ae=${io.resp.bits.ae} replay=${io.resp.bits.replay} data=${Hexadecimal(io.resp.bits.data)}\n")
      }
      when(tl.a.fire) {
        val bund = tl.a.bits
        val sizeInBytes = 1.U << bund.size
        printf(p"A-Channel: ")
        when(bund.opcode === TLMessages.Get) {
          printf(p"Get ${sizeInBytes} bytes from ${Hexadecimal(bund.address)} ")
          printf(p"(source=${bund.source}, mask=${Binary(bund.mask)})\n")
        }.elsewhen(bund.opcode === TLMessages.PutFullData) {
          printf(p"PutFullData ${sizeInBytes} bytes to ${Hexadecimal(bund.address)} ")
          printf(p"(source=${bund.source}, mask=${Binary(bund.mask)})\n")
          printf(p"  data: ${Hexadecimal(bund.data)}\n")
        }.otherwise {
          printf(p"TODO: support A-Channel opcode: ${bund.opcode}\n")
        }
      }
      when(tl.d.fire) {
        val bund = tl.d.bits
        val sizeInBytes = 1.U << bund.size
        printf(p"D-Channel: ")
        when(bund.opcode === TLMessages.AccessAck) {
          printf(p"AccessAck ${sizeInBytes} bytes from ${bund.source}")
          when(bund.denied) { printf(" DENIED") }
          printf("\n")
        }.elsewhen(bund.opcode === TLMessages.AccessAckData) {
          printf(p"AccessAckData ${sizeInBytes} bytes from ${bund.source}")
          when(bund.denied) { printf(" DENIED") }.otherwise {
            printf(p"\n  data: ${Hexadecimal(bund.data)}")
          }
          printf("\n")
        }.otherwise {
          printf(p"TODO: support D-Channel opcode: ${bund.opcode}\n")
        }
      }
    }
  }
}

class ICacheUnitTests extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("ICache")

  val DefaultParams:       Parameters = VerifTestUtils.getVerifParameters()
  val DefaultICacheParams: ICacheParams = DefaultParams(TileKey).icache.get

  it should "elaborate" in {
    val dut = LazyModule(new ICacheStandalone(DefaultICacheParams, DefaultParams))
    test(dut.module) { dut => }
  }
}
