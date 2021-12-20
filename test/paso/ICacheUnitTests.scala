package paso

import chipsalliance.rocketchip.config._
import chiseltest._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._
import freechips.rocketchip.tilelink._
import org.scalatest.flatspec.AnyFlatSpec

class ICacheStandalone(cacheParams: ICacheParams, params: Parameters) extends LazyModule()(params) {
  val cache = LazyModule(new ICache(cacheParams, 0)(p))
  val ioOutNode = BundleBridgeSink[TLBundle]()
  val out = InModuleBody { ioOutNode.makeIO() }
  val bridge = TLToBundleBridge(DefaultTLParams.manager)
  ioOutNode := bridge
  bridge := TLBuffer() := cache.masterNode
  lazy val module = new LazyModuleImp(this) {}
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
