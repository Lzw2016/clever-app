package org.clever.asm;

/**
 * Information about the input stack map frame at the "current" instruction of a method. This is
 * implemented as a Frame subclass for a "basic block" containing only one instruction.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 13:03 <br/>
 */
final class CurrentFrame extends Frame {
    CurrentFrame(final Label owner) {
        super(owner);
    }

    /**
     * Sets this CurrentFrame to the input stack map frame of the next "current" instruction, i.e. the
     * instruction just after the given one. It is assumed that the value of this object when this
     * method is called is the stack map frame status just before the given instruction is executed.
     */
    @Override
    void execute(
            final int opcode, final int arg, final Symbol symbolArg, final SymbolTable symbolTable) {
        super.execute(opcode, arg, symbolArg, symbolTable);
        Frame successor = new Frame(null);
        merge(symbolTable, successor, 0);
        copyFrom(successor);
    }
}
