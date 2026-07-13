package com.senthil.membership.controller;

import com.senthil.membership.model.Membership;
import com.senthil.membership.model.Schedule;
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
        // Jackson's @JsonManagedReference/@JsonBackReference already linked each
        // Schedule.membership back to this instance during deserialization -- no need to
        // re-set it here (doing so via setSchedules(getSchedules()) previously aliased
        // the same list instance and silently wiped it: clear() emptied the very list
        // the for-loop was about to iterate).
        for (Schedule schedule : membership.getSchedules()) {
            schedule.setMembership(membership);
        }
        return membershipRepository.save(membership);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Membership> update(@PathVariable Long id, @Valid @RequestBody Membership membership) {
        if (!membershipRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        membership.setId(id);
        for (Schedule schedule : membership.getSchedules()) {
            schedule.setMembership(membership);
        }
        return ResponseEntity.ok(membershipRepository.save(membership));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        membershipRepository.deleteById(id);
    }
}
