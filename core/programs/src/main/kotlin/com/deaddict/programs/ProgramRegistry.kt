package com.deaddict.programs

interface ProgramRegistry {
    fun all(): List<ProgramDefinition>
    fun find(id: ProgramId): ProgramDefinition?
    fun byCategory(category: ProgramCategory): List<ProgramDefinition>
    fun bySafetyTier(tier: SafetyTier): List<ProgramDefinition>
}

class DefaultProgramRegistry(
    definitions: List<ProgramDefinition> = StandardPrograms.all,
) : ProgramRegistry {
    private val ordered = definitions.toList()
    private val indexed = ordered.associateBy(ProgramDefinition::id)

    init {
        require(ordered.isNotEmpty())
        require(indexed.size == ordered.size) { "Program ids must be unique" }
    }

    override fun all(): List<ProgramDefinition> = ordered

    override fun find(id: ProgramId): ProgramDefinition? = indexed[id]

    override fun byCategory(category: ProgramCategory): List<ProgramDefinition> =
        ordered.filter { it.category == category }

    override fun bySafetyTier(tier: SafetyTier): List<ProgramDefinition> =
        ordered.filter { it.safety.tier == tier }
}

