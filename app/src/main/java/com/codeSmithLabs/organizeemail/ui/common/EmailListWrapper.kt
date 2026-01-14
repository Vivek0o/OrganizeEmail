package com.codeSmithLabs.organizeemail.ui.common

import androidx.compose.runtime.Immutable
import com.codeSmithLabs.organizeemail.data.model.EmailUI

@Immutable
data class EmailListWrapper(
    val emails: List<EmailUI> = emptyList()
)
