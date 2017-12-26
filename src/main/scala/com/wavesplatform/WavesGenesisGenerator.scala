package com.wavesplatform

import com.wavesplatform.settings.WavesSettings
import scorex.account.AddressScheme
import scorex.utils.ScorexLogging
import scorex.wallet.Wallet

object WalletAddressGenerator extends App with ScorexLogging {
  AddressScheme.current = new AddressScheme {
    override val chainId: Byte = 'G'
  }
  val config = Application.readConfig(args.headOption)
  val settings = WavesSettings.fromConfig(config)
  val wallet: Wallet = Wallet(settings.walletSettings)
  val a = wallet.generateNewAccount()
  println(a.get.publicKey.getEncoded)
  println(a.get.address)
}
