package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@NamePattern("%s|skillName")
@Table(name = "ITPEARLS_SKILL_TREE", indexes = {
        @Index(name = "IDX_ITPEARLS_SKILL_TREE", columnList = "SKILL_NAME")
})
@Entity(name = "itpearls_SkillTree")
public class SkillTree extends StandardEntity {
    private static final long serialVersionUID = -1280658717332773151L;

    @NotNull
    @Column(name = "SKILL_NAME", nullable = false, unique = true, length = 80)
    protected String skillName;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SKILL_TREE_ID")
    protected SkillTree skillTree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPEN_POSITION_ID")
    protected OpenPosition openPosition;

    @Composition
    @OnDelete(DeletePolicy.DENY)
    @OneToMany(mappedBy = "skillTree")
    protected List<JobCandidate> candidates;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CANDIDATE_CV_ID")
    protected CandidateCV candidateCV;

    @Lookup(type = LookupType.DROPDOWN, actions = "lookup")
    @ManyToOne
    @JoinColumn(name = "SPECIALISATION_ID")
    protected Specialisation specialisation;

    @Lob
    @Column(name = "COMMENT")
    protected String comment;

    @Column(name = "WIKI_PAGE", length = 250)
    protected String wikiPage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_IMAGE_LOGO")
    protected FileDescriptor fileImageLogo;

    public List<JobCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<JobCandidate> candidates) {
        this.candidates = candidates;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public SkillTree getSkillTree() {
        return skillTree;
    }

    public void setSkillTree(SkillTree skillTree) {
        this.skillTree = skillTree;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public CandidateCV getCandidateCV() {
        return candidateCV;
    }

    public void setCandidateCV(CandidateCV candidateCV) {
        this.candidateCV = candidateCV;
    }

    public void setWikiPage(String wikiPage) {
        this.wikiPage = wikiPage;
    }

    public String getWikiPage() {
        return wikiPage;
    }

    public void setSpecialisation(Specialisation specialisation) {
        this.specialisation = specialisation;
    }

    public Specialisation getSpecialisation() {
        return specialisation;
    }

    public void setFileImageLogo(FileDescriptor fileImageLogo) {
        this.fileImageLogo = fileImageLogo;
    }

    public FileDescriptor getFileImageLogo() {
        return fileImageLogo;
    }
}