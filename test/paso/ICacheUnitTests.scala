package paso

import chipsalliance.rocketchip.config._
import chisel3._
import chiseltest._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.system.DefaultConfig
import freechips.rocketchip.tile._
import freechips.rocketchip.tilelink._
import org.scalatest.flatspec.AnyFlatSpec

class ICacheStandalone(cacheParams: ICacheParams, params: Parameters) extends LazyModule()(params) {

  val cache = LazyModule(new ICache(cacheParams, 0)(p))

  // extract port parameters
  val portParams = cache.masterNode.portParams.head

  val buffer = TLBuffer(BufferParams.default)
  cache.masterNode := buffer

  val bridge = BundleBridgeToTL(portParams)
  buffer := bridge
  val ioInNode = BundleBridgeSource(() => TLBundle(TLBundleParameters(portParams, bridge.edges.out.head.slave)))
  bridge := ioInNode
  val in = InModuleBody { ioInNode.makeIO() }
  lazy val module = new LazyModuleImp(this) {}
}

class ICacheUnitTests extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("ICache")

  val DefaultConfig = new DefaultConfig
  val DefaultTileParams: RocketTileParams = DefaultConfig(RocketTilesKey).head
  // inspired by BaseTile constructor
  val DefaultWithLegacyParams = DefaultConfig.alterMap(
    Map(
      TileKey -> DefaultTileParams,
      TileVisibilityNodeKey -> TLEphemeralNode()(ValName("tile_master"))
    )
  )
  val DefaultICacheParams: ICacheParams = DefaultTileParams.icache.get

  it should "elaborate" in {

    val dut = LazyModule(new ICacheStandalone(DefaultICacheParams, DefaultWithLegacyParams))
    test(dut.module) { dut => }
  }
}
