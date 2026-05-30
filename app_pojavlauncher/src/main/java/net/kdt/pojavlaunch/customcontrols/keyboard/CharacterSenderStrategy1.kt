package net.kdt.pojavlaunch.customcontrols.keyboard

/** Simple interface for sending chars through whatever bridge will be necessary  */
interface CharacterSenderStrategy {
    /** Called when there is a character to delete, may be called multiple times in a row  */
    fun sendBackspace()

    /** Called when we want to send enter specifically  */
    fun sendEnter()

    /** Called when there is a character to send, may be called multiple times in a row  */
    fun sendChar(character: Char)
}
