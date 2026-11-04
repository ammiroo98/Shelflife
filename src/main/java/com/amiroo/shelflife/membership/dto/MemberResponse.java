package com.amiroo.shelflife.membership.dto;

import java.util.UUID;

import com.amiroo.shelflife.membership.Member;
import com.amiroo.shelflife.membership.MemberStatus;

public record MemberResponse(UUID id, String name, MemberStatus status) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getStatus());
    }
}
