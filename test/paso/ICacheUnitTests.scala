package paso

import chipsalliance.rocketchip.config._
import chiseltest._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._
import freechips.rocketchip.tilelink._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import freechips.rocketchip.formal.MonitorDirection

class ICacheStandalone(cacheParams: ICacheParams, params: Parameters) extends LazyModule()(params) {
  val cache = LazyModule(new ICache(cacheParams, 0)(p))
  val ioOutNode = BundleBridgeSink[TLBundle]()
  val bridge = TLToBundleBridge(DefaultTLParams.manager)
  ioOutNode := bridge
  bridge := TLBuffer() := cache.masterNode
  lazy val module = new ICacheStandaloneImpl(this)
}

class ICacheStandaloneImpl(w: ICacheStandalone) extends LazyModuleImp(w) {
  // TileLink I/O
  //val tl = IO(chiselTypeOf(w.ioOutNode.bundle))
  //tl <> w.ioOutNode.bundle

  // TileLink Monitor
  val (bund, edge) = w.bridge.in.head
  val monitor = Module(new TLMonitor(TLMonitorArgs(edge), MonitorDirection.Receiver))
  monitor.io.in := w.ioOutNode.bundle



  // we assume that bits are never corrupted
  // when(tl.d.fire) { assume(!tl.d.bits.corrupt) }

  // other (frontend) I/O
  val io = IO(chiselTypeOf(w.cache.module.io))
  io <> w.cache.module.io
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
