package verif

import chipsalliance.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.prci.ClockSinkParameters
import freechips.rocketchip.rocket._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tile._
import freechips.rocketchip.tilelink._

// src: https://github.com/TsaiAnson/verif/blob/master/tilelink/test/StandaloneBlocks.scala
object DefaultTLParams {
  def manager: TLSlavePortParameters = TLSlavePortParameters.v1(
    Seq(
      TLSlaveParameters.v1( // TL-UH master
        address = Seq(AddressSet(0x0, 0xfff)),
        supportsGet = TransferSizes(1, 32),
        supportsPutFull = TransferSizes(1, 32),
        supportsPutPartial = TransferSizes(1, 32),
        supportsLogical = TransferSizes(1, 32),
        supportsArithmetic = TransferSizes(1, 32),
        supportsHint = TransferSizes(1, 32),
        regionType = RegionType.UNCACHED
      )
    ),
    beatBytes = 8
  )

  def client(name: String = "TLMasterPort", idRange: IdRange = IdRange(0, 1)): TLMasterPortParameters =
    TLMasterPortParameters.v1(
      Seq(
        TLMasterParameters.v1(name = name, sourceId = idRange)
      )
    )
}

/** Dummy tile params for out of context elaboration
  */
case object VerifTileParams extends TileParams {
  val name:            Option[String] = Some("verif_tile")
  val hartId:          Int = 0
  val core:            RocketCoreParams = RocketCoreParams()
  val clockSinkParams: ClockSinkParameters = ClockSinkParameters()
  val beuAddr:         Option[BigInt] = None
  val blockerCtrlAddr: Option[BigInt] = None
  val btb:             Option[BTBParams] = None
  val dcache: Option[DCacheParams] = Some(
    DCacheParams(rowBits = 128)
  ) // TODO: can these be derived from beat bytes, etc
  val icache: Option[ICacheParams] = Some(ICacheParams(rowBits = 128))
}

class VerifBaseRocketConfig
    extends Config(
      new freechips.rocketchip.subsystem.WithNBigCores(1) ++ // single rocket-core
        new freechips.rocketchip.subsystem.WithJtagDTM ++ // set the debug module to expose a JTAG port
        new freechips.rocketchip.subsystem.WithNoMMIOPort ++ // no top-level MMIO master port (overrides default set in rocketchip)
        new freechips.rocketchip.subsystem.WithNoSlavePort ++ // no top-level MMIO slave port (overrides default set in rocketchip)
        //new freechips.rocketchip.subsystem.WithInclusiveCache ++       // use Sifive L2 cache TODO: Why doesn't this work
        new freechips.rocketchip.subsystem.WithoutTLMonitors ++ // single rocket-core
        new freechips.rocketchip.subsystem.WithNExtTopInterrupts(0) ++ // no external interrupts
        new freechips.rocketchip.system.BaseConfig
    ) // "base" rocketchip system

// https://github.com/TsaiAnson/verif/blob/master/cosim/src/CosimTestUtils.scala
object VerifTestUtils {
  def getVerifTLMasterPortParameters(): TLMasterPortParameters = {
    TLMasterPortParameters.v1(Seq(TLMasterParameters.v1("bundleBridgeToTL")))
  }

  def getVerifTLSlavePortParameters(
    beatBytes:    Int = 16,
    pAddrBits:    Int = 32,
    transferSize: TransferSizes = TransferSizes(1, 64)
  ): TLSlavePortParameters = {
    TLSlavePortParameters.v1(
      Seq(
        TLSlaveParameters.v1(
          address = Seq(AddressSet(0x0, BigInt("1" * pAddrBits, 2))),
          supportsGet = transferSize,
          supportsPutFull = transferSize,
          supportsPutPartial = transferSize
        )
      ),
      beatBytes
    )
  }

  def getVerifTLBundleParameters(
    beatBytes:    Int = 16,
    pAddrBits:    Int = 32,
    transferSize: TransferSizes = TransferSizes(1, 64)
  ): TLBundleParameters = {
    TLBundleParameters(
      getVerifTLMasterPortParameters(),
      getVerifTLSlavePortParameters(beatBytes, pAddrBits, transferSize)
    ).copy(sourceBits =
      5
    ) //TOD: This is a hack, need to add some way to specify source bits in the bundle creaion process
  }

  def getVerifParameters(
    xLen:         Int = 64,
    beatBytes:    Int = 16,
    blockBytes:   Int = 64,
    pAddrBits:    Int = 32,
    transferSize: TransferSizes = TransferSizes(1, 64)
  ): Parameters = {

    val origParams = new VerifBaseRocketConfig

    // augment the parameters
    implicit val p = origParams.alterPartial {
      case MonitorsEnabled => false
      case TileKey         => VerifTileParams
      case XLen            => xLen // Have to use xLen to avoid errors with naming
      case PgLevels        => if (xLen == 64) 3 else 2
      case MaxHartIdBits   => 1
      case SystemBusKey =>
        SystemBusParams(
          beatBytes = beatBytes,
          blockBytes = blockBytes
        )
    }

    // TODO: should these be args to the main function as well for ease of use?
    def verifTLUBundleParams: TLBundleParameters = TLBundleParameters(
      addressBits = 64,
      dataBits = 64,
      sourceBits = 1,
      sinkBits = 1,
      sizeBits = 6,
      echoFields = Seq(),
      requestFields = Seq(),
      responseFields = Seq(),
      hasBCE = false
    )

    val dummyInNode = BundleBridgeSource(() => TLBundle(verifTLUBundleParams))
    val dummyOutNode = BundleBridgeSink[TLBundle]()

    val tlMasterXbar = LazyModule(new TLXbar)
    val visibilityNode = TLEphemeralNode()(ValName("tile_master"))

    visibilityNode :=* tlMasterXbar.node
    tlMasterXbar.node :=
      BundleBridgeToTL(getVerifTLMasterPortParameters()) :=
      dummyInNode

    dummyOutNode :=
      TLToBundleBridge(getVerifTLSlavePortParameters(beatBytes, pAddrBits, transferSize)) :=
      visibilityNode

    val outParams = p.alterPartial { case TileVisibilityNodeKey =>
      visibilityNode
    }

    outParams
  }
}
