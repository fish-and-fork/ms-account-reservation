package com.example.ms_account_reservation.specification;

import com.example.ms_account_reservation.entity.Client;
import com.example.ms_account_reservation.model.ClientType;
import org.springframework.data.jpa.domain.Specification;

public final class ClientSpecifications {

    private ClientSpecifications() {}

    public static Specification<Client> notDeleted() {
        return (root, query, cb) ->
                cb.notEqual(root.get("clientType"), ClientType.DELETED);
    }

    public static Specification<Client> lastNameContains(String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + lastName.toLowerCase() + "%";
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("lastName")), pattern);
    }

    public static Specification<Client> hasMdmCode(Long mdmCode) {
        if (mdmCode == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("mdmCode"), mdmCode);
    }
}
