package paso

import chipsalliance.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.rocket.ICacheParams
import freechips.rocketchip.tile.TileKey

import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec

class ICacheFormalTests extends AnyFlatSpec with ChiselScalatestTester with Formal {
  behavior.of("ICache")

  val DefaultParams:       Parameters = VerifTestUtils.getVerifParameters()
  val DefaultICacheParams: ICacheParams = DefaultParams(TileKey).icache.get

  it should "bmc for a couple of cycles" in {
    val dut = LazyModule(new ICacheStandalone(DefaultICacheParams, DefaultParams))

    verify(dut.module, Seq(BoundedCheck(10), BtormcEngineAnnotation))

    val e = intercept[FailedBoundedCheckException] {
      verify(dut.module, Seq(BoundedCheck(10), BtormcEngineAnnotation))
    }
    assert(e.failAt == 5) // fails after 6 steps
  }
}
