package com

import com.wavesplatform.settings.WavesSettings
import com.wavesplatform.utils.forceStopApplication
import scorex.block.Block
import scorex.transaction.{BlockchainUpdater, History}
import scorex.utils.ScorexLogging
import scorex.wallet.Wallet

package object wavesplatform extends ScorexLogging {
  def checkGenesis(history: History, settings: WavesSettings, blockchainUpdater: BlockchainUpdater, wallet: Wallet): Unit = if (history.isEmpty) {
    // todo(gost): don't add the genesis if you public key is not from the config file
    Block.genesis(settings.blockchainSettings.genesisSettings, wallet).flatMap(blockchainUpdater.processBlock)
      .left.foreach { value =>
      log.error(value.toString)
      forceStopApplication()
    }
    log.info("Genesis block has been added to the state")
  }
}
