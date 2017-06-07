package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.CATCH_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.DO_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.ELSE_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.FINALLY_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.FOR_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.IF_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.TRY_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.WHEN_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.WHILE_KEYWORD
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf

class SpacingAroundKeywordRule : Rule("keyword-spacing") {

    private val noLFBeforeSet = TokenSet.create(ELSE_KEYWORD, CATCH_KEYWORD, FINALLY_KEYWORD)
    private val tokenSet = TokenSet.create(FOR_KEYWORD, IF_KEYWORD, ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD,
        TRY_KEYWORD, CATCH_KEYWORD, FINALLY_KEYWORD, WHEN_KEYWORD)

    private val keywordsWithoutSpaces = TokenSet.create(KtTokens.GET_KEYWORD, KtTokens.SET_KEYWORD)

    override fun visit(node: ASTNode, autoCorrect: Boolean,
                       emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {

        if (node is LeafPsiElement) {
            if (tokenSet.contains(node.elementType) && node.nextLeaf() !is PsiWhiteSpace) {
                emit(node.startOffset + node.text.length, "Missing spacing after \"${node.text}\"", true)
                if (autoCorrect) {
                    node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                }
            } else if (keywordsWithoutSpaces.contains(node.elementType) && node.nextLeaf() is PsiWhiteSpace) {
                val parent = node.parent
                if (parent is KtPropertyAccessor && parent.hasBody()) {
                    emit(node.startOffset, "Unexpected spacing after \"${node.text}\"", true)
                    if (autoCorrect) {
                        node.nextLeaf()?.delete()
                    }
                }
            } else if (noLFBeforeSet.contains(node.elementType)) {
                val prevLeaf = PsiTreeUtil.prevLeaf(node)
                if (prevLeaf is PsiWhiteSpaceImpl && prevLeaf.textContains('\n') &&
                    (node.elementType != ELSE_KEYWORD || node.parent !is KtWhenEntry) &&
                    (PsiTreeUtil.prevLeaf(prevLeaf)?.textMatches("}") ?: false)) {
                    emit(node.startOffset, "Unexpected newline before \"${node.text}\"", true)
                    if (autoCorrect) {
                        prevLeaf.rawReplaceWithText(" ")
                    }
                }
            }
        }
    }
}
