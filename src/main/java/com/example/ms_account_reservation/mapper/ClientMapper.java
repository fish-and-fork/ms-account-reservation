package com.example.ms_account_reservation.mapper;

import com.example.dto.ClientCreateRequest;
import com.example.dto.ClientResponse;
import com.example.ms_account_reservation.entity.Client;
import com.example.ms_account_reservation.model.ClientType;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ClientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "clientType", constant = "ACTIVE")
    Client toEntity(ClientCreateRequest request);

    @Mapping(target = "hasAccounts", ignore = true)
    ClientResponse toResponse(Client client);

    @ValueMapping(source = "DELETED", target = "DELETED")
    @ValueMapping(source = "ACTIVE", target = "ACTIVE")
    @ValueMapping(source = "BLOCKED", target = "BLOCKED")
    ClientResponse.ClientTypeEnum mapClientType(ClientType clientType);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "clientType", constant = "ACTIVE")
    void updateEntity(ClientCreateRequest request, @MappingTarget Client client);
}
