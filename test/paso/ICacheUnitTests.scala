package paso


import chipsalliance.rocketchip.config._
import chisel3._
import chiseltest._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.subsystem.{CacheBlockBytes, RocketTilesKey, SystemBusKey, WithoutTLMonitors}
import freechips.rocketchip.system.DefaultConfig
import freechips.rocketchip.tile.{RocketTileParams, TileKey}
import org.scalatest.flatspec.AnyFlatSpec

class ICacheWrapper(cacheParams: ICacheParams, p: Parameters) extends Module {
  val cache = LazyModule(new ICache(cacheParams, 0)(p))
  val dut = cache.module
  val io = IO(chiselTypeOf(dut.io))
  io <> dut.io
}


class ICacheUnitTests extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "ICache"




  val DefaultConfig = new DefaultConfig
  val DefaultTileParams: RocketTileParams = DefaultConfig(RocketTilesKey).head
  val DefaultWithTileKey = DefaultConfig.alterMap(Map(TileKey -> DefaultTileParams))
  val DefaultICacheParams: ICacheParams = DefaultTileParams.icache.get


  it should "elaborate" in {

    test(new ICacheWrapper(DefaultICacheParams, DefaultWithTileKey)) { dut =>

    }
  }
}
