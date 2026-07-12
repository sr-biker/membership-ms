package com.senthil.membership.controller;

import com.senthil.membership.model.Membership;
import com.senthil.membership.repository.MembershipRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {

    private final MembershipRepository membershipRepository;

    public MembershipController(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @GetMapping
    public List<Membership> list() {
        return membershipRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Membership> get(@PathVariable Long id) {
        return membershipRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Membership create(@Valid @RequestBody Membership membership) {
        membership.setId(null);
        membership.setSchedules(membership.getSchedules());
        return membershipRepository.save(membership);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Membership> update(@PathVariable Long id, @Valid @RequestBody Membership membership) {
        if (!membershipRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        membership.setId(id);
        membership.setSchedules(membership.getSchedules());
        return ResponseEntity.ok(membershipRepository.save(membership));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        membershipRepository.deleteById(id);
    }
}
