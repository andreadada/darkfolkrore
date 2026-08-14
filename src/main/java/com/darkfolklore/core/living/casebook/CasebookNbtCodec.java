package com.darkfolklore.core.living.casebook;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.magic.MagicTradition;
import com.darkfolklore.core.persistence.WorldPosition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.*;

final class CasebookNbtCodec {
    private CasebookNbtCodec() {}

    static CompoundTag save(InvestigationCaseRecord value) {
        CompoundTag row = new CompoundTag();
        row.putUUID("id", value.id()); row.putUUID("player", value.player()); row.putUUID("contract", value.contractId());
        value.storyId().ifPresent(id -> row.putUUID("story", id));
        row.putString("origin", value.origin().name()); row.putString("stage", value.stage().name());
        row.putString("dimension", value.anchor().dimension()); row.putInt("x", value.anchor().x());
        row.putInt("y", value.anchor().y()); row.putInt("z", value.anchor().z());
        row.putLong("created", value.createdAt()); row.putLong("updated", value.updatedAt()); row.putLong("expires", value.expiresAt());
        value.identifiedConcept().ifPresent(concept -> row.putString("identified", concept));
        ListTag evidence = new ListTag();
        value.evidence().forEach(type -> { CompoundTag e = new CompoundTag(); e.putString("id", type.name()); evidence.add(e); });
        row.put("evidence", evidence);
        ListTag notes = new ListTag(); value.notes().forEach(note -> notes.add(saveNote(note))); row.put("notes", notes);
        return row;
    }

    static InvestigationCaseRecord read(CompoundTag row) {
        UUID id = row.getUUID("id"), player = row.getUUID("player"), contract = row.getUUID("contract");
        Optional<UUID> story = row.hasUUID("story") ? Optional.of(row.getUUID("story")) : Optional.empty();
        WorldPosition anchor = new WorldPosition(row.getString("dimension"), row.getInt("x"), row.getInt("y"), row.getInt("z"));
        InvestigationCaseRecord value = new InvestigationCaseRecord(id, player, contract, story,
                enumValue(CaseOrigin.class,row.getString("origin"),CaseOrigin.CONTRACT), anchor,
                row.getLong("created"), row.getLong("expires"));
        EnumSet<EvidenceType> evidence = EnumSet.noneOf(EvidenceType.class);
        ListTag evidenceRows = row.getList("evidence", Tag.TAG_COMPOUND);
        for (int i=0;i<evidenceRows.size();i++) optionalEnum(EvidenceType.class,evidenceRows.getCompound(i).getString("id")).ifPresent(evidence::add);
        List<CaseNote> notes = new ArrayList<>();
        ListTag noteRows = row.getList("notes", Tag.TAG_COMPOUND);
        for (int i=Math.max(0,noteRows.size()-InvestigationCaseRecord.HARD_MAX_NOTES);i<noteRows.size();i++) notes.add(readNote(noteRows.getCompound(i)));
        value.restore(enumValue(CaseStage.class,row.getString("stage"),CaseStage.INVESTIGATING), evidence, notes,
                row.getString("identified"), row.getLong("updated"));
        return value;
    }

    private static CompoundTag saveNote(CaseNote note) {
        CompoundTag row = new CompoundTag();
        row.putLong("time", note.gameTime()); row.putString("kind", note.kind().name()); row.putString("detail", note.detail());
        row.putFloat("confidence", note.confidence());
        note.source().ifPresent(id -> row.putUUID("source", id));
        note.evidence().ifPresent(type -> row.putString("evidence", type.name()));
        note.tradition().ifPresent(type -> row.putString("tradition", type.name()));
        return row;
    }

    private static CaseNote readNote(CompoundTag row) {
        Optional<UUID> source = row.hasUUID("source") ? Optional.of(row.getUUID("source")) : Optional.empty();
        return new CaseNote(row.getLong("time"), enumValue(CaseNoteKind.class,row.getString("kind"),CaseNoteKind.SOCIAL),
                row.getString("detail"), source, optionalEnum(EvidenceType.class,row.getString("evidence")),
                optionalEnum(MagicTradition.class,row.getString("tradition")), row.getFloat("confidence"));
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String name, E fallback) {
        try { return Enum.valueOf(type,name); } catch (RuntimeException ex) { return fallback; }
    }
    private static <E extends Enum<E>> Optional<E> optionalEnum(Class<E> type, String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        try { return Optional.of(Enum.valueOf(type,name)); } catch (RuntimeException ex) { return Optional.empty(); }
    }
}
