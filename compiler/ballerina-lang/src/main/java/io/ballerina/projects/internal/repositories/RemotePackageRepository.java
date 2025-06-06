package io.ballerina.projects.internal.repositories;

import io.ballerina.projects.DependencyGraph;
import io.ballerina.projects.JvmTarget;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.ProjectException;
import io.ballerina.projects.Settings;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.PackageLockingMode;
import io.ballerina.projects.environment.PackageMetadataResponse;
import io.ballerina.projects.environment.PackageRepository;
import io.ballerina.projects.environment.ResolutionOptions;
import io.ballerina.projects.environment.ResolutionRequest;
import io.ballerina.projects.environment.ResolutionResponse;
import io.ballerina.projects.internal.ImportModuleRequest;
import io.ballerina.projects.internal.ImportModuleResponse;
import org.ballerinalang.central.client.CentralAPIClient;
import org.ballerinalang.central.client.CentralClientConstants;
import org.ballerinalang.central.client.exceptions.CentralClientException;
import org.ballerinalang.central.client.exceptions.ConnectionErrorException;
import org.ballerinalang.central.client.model.PackageNameResolutionRequest;
import org.ballerinalang.central.client.model.PackageNameResolutionResponse;
import org.ballerinalang.central.client.model.PackageResolutionRequest;
import org.ballerinalang.central.client.model.PackageResolutionResponse;
import org.wso2.ballerinalang.util.RepoUtils;

import java.io.PrintStream;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.ballerina.projects.DependencyGraph.DependencyGraphBuilder.getBuilder;
import static io.ballerina.projects.util.ProjectUtils.getAccessTokenOfCLI;
import static io.ballerina.projects.util.ProjectUtils.getLatest;
import static io.ballerina.projects.util.ProjectUtils.initializeProxy;

/**
 * This class represents the remote package repository.
 *
 * @since 2.0.0
 */
public class RemotePackageRepository implements PackageRepository {

    private final FileSystemRepository fileSystemRepo;
    private final CentralAPIClient client;

    public RemotePackageRepository(FileSystemRepository fileSystemRepo, CentralAPIClient client) {
        this.fileSystemRepo = fileSystemRepo;
        this.client = client;
    }

    public static RemotePackageRepository from(Environment environment, Path cacheDirectory, String repoUrl,
                                               Settings settings) {
        if (Files.notExists(cacheDirectory)) {
            throw new ProjectException("cache directory does not exists: " + cacheDirectory);
        }
        String ballerinaShortVersion = RepoUtils.getBallerinaShortVersion();
        FileSystemRepository fileSystemRepository = new FileSystemRepository(
                environment, cacheDirectory, ballerinaShortVersion);
        Proxy proxy = initializeProxy(settings.getProxy());
        CentralAPIClient client = new CentralAPIClient(repoUrl, proxy, settings.getProxy().username(),
                settings.getProxy().password(), getAccessTokenOfCLI(settings),
                settings.getCentral().getConnectTimeout(),
                settings.getCentral().getReadTimeout(), settings.getCentral().getWriteTimeout(),
                settings.getCentral().getCallTimeout(), settings.getCentral().getMaxRetries());
        return new RemotePackageRepository(fileSystemRepository, client);
    }

    public static RemotePackageRepository from(Environment environment, Path cacheDirectory, Settings settings) {
        String repoUrl = RepoUtils.getRemoteRepoURL();
        if ("".equals(repoUrl)) {
            throw new ProjectException("remote repo url is empty");
        }

        return from(environment, cacheDirectory, repoUrl, settings);
    }

    @Override
    public Optional<Package> getPackage(ResolutionRequest request, ResolutionOptions options) {
        // Check if the package is in cache
        Optional<Package> cachedPackage = this.fileSystemRepo.getPackage(request, options);
        if (cachedPackage.isPresent()) {
            return cachedPackage;
        }

        String packageName = request.packageName().value();
        String orgName = request.orgName().value();
        String version = request.version().isPresent() ? request.version().get().toString() : null;

        Path packagePathInBalaCache = this.fileSystemRepo.bala.resolve(orgName).resolve(packageName);

        // If environment is online pull from central
        if (!options.offline()) {
            String supportedPlatform = Arrays.stream(JvmTarget.values())
                    .map(target -> target.code())
                    .collect(Collectors.joining(","));
            try {
                this.client.pullPackage(orgName, packageName, version, packagePathInBalaCache, supportedPlatform,
                        RepoUtils.getBallerinaVersion(), true);
            } catch (CentralClientException e) {
                boolean enableOutputStream =
                        Boolean.parseBoolean(System.getProperty(CentralClientConstants.ENABLE_OUTPUT_STREAM));
                if (enableOutputStream) {
                    final PrintStream out = System.out;
                    out.println("Error while pulling package [" + orgName + "/" + packageName + ":" + version +
                            "]: " + e.getMessage());

                }
            }
        }

        return this.fileSystemRepo.getPackage(request, options);
    }

    @Override
    public Collection<PackageVersion> getPackageVersions(ResolutionRequest request, ResolutionOptions options) {
        String langRepoBuild = System.getProperty("LANG_REPO_BUILD");
        if (langRepoBuild != null) {
            return Collections.emptyList();
        }
        String orgName = request.orgName().value();
        String packageName = request.packageName().value();

        // First, Get local versions
        Set<PackageVersion> packageVersions = new HashSet<>(fileSystemRepo.getPackageVersions(request, options));

        // If the resolution request specifies to resolve offline, we return the local version
        if (options.offline()) {
            return new ArrayList<>(packageVersions);
        }

        try {
            String supportedPlatform = Arrays.stream(JvmTarget.values())
                    .map(target -> target.code())
                    .collect(Collectors.joining(","));
            for (String version : this.client.getPackageVersions(orgName, packageName, supportedPlatform,
                    RepoUtils.getBallerinaVersion())) {
                packageVersions.add(PackageVersion.from(version));
            }

        } catch (ConnectionErrorException e) {
            // ignore connect to remote repo failure
            return new ArrayList<>(packageVersions);
        } catch (CentralClientException e) {
            throw new ProjectException(e.getMessage());
        }
        return new ArrayList<>(packageVersions);
    }

    @Override
    public Map<String, List<String>> getPackages() {
        // We only return locally cached packages
        return fileSystemRepo.getPackages();
    }

    @Override
    public Collection<ImportModuleResponse> getPackageNames(Collection<ImportModuleRequest> requests,
                                                            ResolutionOptions options) {
        Collection<ImportModuleResponse> filesystem = fileSystemRepo.getPackageNames(requests, options);
        if (options.offline()) {
            return filesystem;
        }

        try {
            List<ImportModuleResponse> remote = new ArrayList<>();
            PackageNameResolutionRequest resolutionRequest = toPackageNameResolutionRequest(requests);
            String supportedPlatform = Arrays.stream(JvmTarget.values())
                    .map(target -> target.code())
                    .collect(Collectors.joining(","));
            PackageNameResolutionResponse response = this.client.resolvePackageNames(resolutionRequest,
                    supportedPlatform, RepoUtils.getBallerinaVersion());
            remote.addAll(toImportModuleResponses(requests, response));

            return mergeNameResolution(filesystem, remote);
        } catch (ConnectionErrorException e) {
            // ignore connect to remote repo failure
            // TODO we need to add diagnostics for resolution errors
        } catch (CentralClientException e) {
            throw new ProjectException(e.getMessage());
        }
        return filesystem;
    }

    private List<ImportModuleResponse> mergeNameResolution(Collection<ImportModuleResponse> filesystem,
                                                           Collection<ImportModuleResponse> remote) {
        return new ArrayList<>(
            Stream.of(filesystem, remote)
                .flatMap(Collection::stream).collect(Collectors.toMap(
                    ImportModuleResponse::importModuleRequest, Function.identity(),
                    (ImportModuleResponse x, ImportModuleResponse y) -> {
                        if (y.resolutionStatus().equals(ResolutionResponse.ResolutionStatus.UNRESOLVED)) {
                            return x;
                        } else if (x.resolutionStatus().equals(ResolutionResponse.ResolutionStatus.UNRESOLVED)) {
                            return y;
                        } else if (getLatest(x.packageDescriptor().version(),
                                y.packageDescriptor().version()).equals(
                                y.packageDescriptor().version())) {
                            return y;
                        }
                        return x;
                    })).values());
    }

    private List<ImportModuleResponse> toImportModuleResponses(Collection<ImportModuleRequest> requests,
                                                               PackageNameResolutionResponse response) {
        List<ImportModuleResponse> result = new ArrayList<>();
        for (ImportModuleRequest module : requests) {
            PackageOrg packageOrg = module.packageOrg();
            String moduleName = module.moduleName();
            Optional<PackageNameResolutionResponse.Module> resolvedModule = response.resolvedModules().stream()
                    .filter(m -> m.getModuleName().equals(moduleName)
                            && m.getOrganization().equals(packageOrg.value())).findFirst();
            if (resolvedModule.isPresent()) {
                PackageDescriptor packageDescriptor = PackageDescriptor.from(packageOrg,
                        PackageName.from(resolvedModule.get().getPackageName()),
                        PackageVersion.from(resolvedModule.get().getVersion()));
                ImportModuleResponse importModuleResponse = new ImportModuleResponse(packageDescriptor, module);
                result.add(importModuleResponse);
            } else {
                result.add(new ImportModuleResponse(module));
            }
        }
        return result;
    }

    private PackageNameResolutionRequest toPackageNameResolutionRequest(Collection<ImportModuleRequest> unresolved) {
        PackageNameResolutionRequest request = new PackageNameResolutionRequest();
        for (ImportModuleRequest module : unresolved) {
            if (module.possiblePackages().isEmpty()) {
                request.addModule(module.packageOrg().value(),
                        module.moduleName());
                continue;
            }
            List<PackageNameResolutionRequest.Module.PossiblePackage> possiblePackages = new ArrayList<>();
            for (PackageDescriptor possiblePackage : module.possiblePackages()) {
                possiblePackages.add(new PackageNameResolutionRequest.Module.PossiblePackage(
                        possiblePackage.org().toString(),
                        possiblePackage.name().toString(),
                        possiblePackage.version().toString()));
            }
            request.addModule(module.packageOrg().value(),
                    module.moduleName(), possiblePackages, PackageResolutionRequest.Mode.MEDIUM);
        }
        return request;
    }

    @Override
    public Collection<PackageMetadataResponse> getPackageMetadata(Collection<ResolutionRequest> requests,
                                                                  ResolutionOptions options) {
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }

        // Resolve all the requests locally
        Collection<PackageMetadataResponse> cachedPackages = fileSystemRepo.getPackageMetadata(requests, options);
        List<PackageMetadataResponse> deprecatedPackages = new ArrayList<>();
        if (options.offline()) {
            return cachedPackages;
        }
        List<ResolutionRequest> updatedRequests = new ArrayList<>(requests);
        // Remove the already resolved requests when the locking mode is hard
        for (PackageMetadataResponse response : cachedPackages) {
            if (response.packageLoadRequest().version().isPresent()
                    && response.packageLoadRequest().packageLockingMode().equals(PackageLockingMode.HARD)
                    && response.resolutionStatus().equals(ResolutionResponse.ResolutionStatus.RESOLVED)) {
                updatedRequests.remove(response.packageLoadRequest());
            }
            if (response.resolutionStatus().equals(ResolutionResponse.ResolutionStatus.RESOLVED)) {
                Optional<Package> pkg = fileSystemRepo.getPackage(response.packageLoadRequest(), options);
                if (pkg.isPresent() && pkg.get().descriptor().getDeprecated()) {
                    deprecatedPackages.add(response);
                }
            }
        }
        // Resolve the requests from remote repository if there are unresolved requests
        if (!updatedRequests.isEmpty()) {
            try {
                PackageResolutionRequest packageResolutionRequest = toPackageResolutionRequest(updatedRequests);
                Collection<PackageMetadataResponse> remotePackages =
                        fromPackageResolutionResponse(updatedRequests, packageResolutionRequest);
                // Merge central requests and local requests
                // Here we will pick the latest package from remote or local
                return mergeResolution(remotePackages, cachedPackages, deprecatedPackages);

            } catch (ConnectionErrorException e) {
                // ignore connect to remote repo failure
                // TODO we need to add diagnostics for resolution errors
            } catch (CentralClientException e) {
                throw new ProjectException(e.getMessage());
            }
        }
        // Return cachedPackages when central requests are not performed
        return cachedPackages;
    }

    private Collection<PackageMetadataResponse> mergeResolution(
            Collection<PackageMetadataResponse> remoteResolution, Collection<PackageMetadataResponse> filesystem,
            List<PackageMetadataResponse> deprecatedPackages) {
        List<PackageMetadataResponse> mergedResults = new ArrayList<>(
                Stream.of(filesystem, remoteResolution)
                        .flatMap(Collection::stream).collect(Collectors.toMap(
                        PackageMetadataResponse::packageLoadRequest, Function.identity(),
                        (PackageMetadataResponse x, PackageMetadataResponse y) -> {
                            if (y.resolutionStatus().equals(ResolutionResponse.ResolutionStatus.UNRESOLVED)) {
                                // filesystem response is resolved &  remote response is unresolved
                                return x;
                            } else if (x.resolutionStatus().equals(ResolutionResponse.ResolutionStatus.UNRESOLVED)) {
                                // filesystem response is unresolved &  remote response is resolved
                                return y;
                            } else if (x.resolvedDescriptor().version().equals(y.resolvedDescriptor().version())) {
                                // Both responses have the same version and there is a mismatch in deprecated status,
                                // we need to update the deprecated status in the file system repo
                                // to match the remote repo as it is the most up to date.
                                if (deprecatedPackages != null && y.resolvedDescriptor() != null &&
                                        deprecatedPackages.contains(x) ^ y.resolvedDescriptor().getDeprecated()) {
                                    fileSystemRepo.updateDeprecatedStatusForPackage(y.resolvedDescriptor());
                                }
                                return x;
                            }
                            // x not deprecate & y not deprecate
                            //      - x is the latest : return x (this will not happen in real)
                            //      - y is the latest : return y
                            // x not deprecated & y deprecated
                            //      - x is the latest : outdated. return y
                            //      - y is the latest : return y
                            // x deprecated & y not deprecated
                            //      - x is the latest : outdated. return y
                            //      - y is the latest : return y
                            // x deprecated & y deprecated
                            //      - x is the latest : not possible
                            //      - y is the latest : return y

                            // If the equivalent package is available in the file system repo,
                            // try to update the deprecated status.
                            // Because if available in cache, it won't be pulled.
                            fileSystemRepo.updateDeprecatedStatusForPackage(y.resolvedDescriptor());
                            return y;
                        })).values());
        return mergedResults;
    }

    private Collection<PackageMetadataResponse> fromPackageResolutionResponse(
            Collection<ResolutionRequest> packageLoadRequests, PackageResolutionRequest packageResolutionRequest)
            throws CentralClientException {
        List<PackageMetadataResponse> response = new ArrayList<>();
        Set<ResolutionRequest> resolvedRequests = new HashSet<>();
        String supportedPlatform = Arrays.stream(JvmTarget.values())
                .map(target -> target.code())
                .collect(Collectors.joining(","));
        PackageResolutionResponse packageResolutionResponse = client.resolveDependencies(
                packageResolutionRequest, supportedPlatform, RepoUtils.getBallerinaVersion());
        for (ResolutionRequest resolutionRequest : packageLoadRequests) {
            if (resolvedRequests.contains(resolutionRequest)) {
                continue;
            }
            // find response from server
            // checked in resolved group
            Optional<PackageResolutionResponse.Package> match = packageResolutionResponse.resolved().stream()
                    .filter(p -> p.name().equals(resolutionRequest.packageName().value()) &&
                            p.org().equals(resolutionRequest.orgName().value())).findFirst();
            // If we found a match we will add it to response
            if (match.isPresent()) {
                PackageVersion version = PackageVersion.from(match.get().version());
                DependencyGraph<PackageDescriptor> dependencies = createPackageDependencyGraph(match.get());
                PackageDescriptor packageDescriptor = PackageDescriptor.from(resolutionRequest.orgName(),
                        resolutionRequest.packageName(),
                        version, match.get().getDeprecated(), match.get().getDeprecateMessage());
                PackageMetadataResponse responseDescriptor = PackageMetadataResponse.from(resolutionRequest,
                        packageDescriptor,
                        dependencies);
                response.add(responseDescriptor);
                resolvedRequests.add(resolutionRequest);
            } else {
                // If the package is not in resolved for all jvm platforms we assume the package is unresolved
                response.add(PackageMetadataResponse.createUnresolvedResponse(resolutionRequest));
            }
        }

        return response;
    }

    private static DependencyGraph<PackageDescriptor> createPackageDependencyGraph(
            PackageResolutionResponse.Package aPackage) {
        DependencyGraph.DependencyGraphBuilder<PackageDescriptor> graphBuilder = getBuilder();

        for (PackageResolutionResponse.Dependency dependency : aPackage.dependencyGraph()) {
            PackageDescriptor pkg = PackageDescriptor.from(PackageOrg.from(dependency.org()),
                    PackageName.from(dependency.name()), PackageVersion.from(dependency.version()));
            Set<PackageDescriptor> dependentPackages = new HashSet<>();
            for (PackageResolutionResponse.Dependency dependencyPkg : dependency.dependencies()) {
                dependentPackages.add(PackageDescriptor.from(PackageOrg.from(dependencyPkg.org()),
                        PackageName.from(dependencyPkg.name()),
                        PackageVersion.from(dependencyPkg.version())));
            }
            graphBuilder.addDependencies(pkg, dependentPackages);
        }

        return graphBuilder.build();
    }

    private PackageResolutionRequest toPackageResolutionRequest(Collection<ResolutionRequest> resolutionRequests) {
        PackageResolutionRequest packageResolutionRequest = new PackageResolutionRequest();
        for (ResolutionRequest resolutionRequest : resolutionRequests) {
            PackageResolutionRequest.Mode mode = switch (resolutionRequest.packageLockingMode()) {
                case HARD -> PackageResolutionRequest.Mode.HARD;
                case MEDIUM -> PackageResolutionRequest.Mode.MEDIUM;
                case SOFT -> PackageResolutionRequest.Mode.SOFT;
            };
            String version = resolutionRequest.version().map(v -> v.value().toString()).orElse("");
            packageResolutionRequest.addPackage(resolutionRequest.orgName().value(),
                    resolutionRequest.packageName().value(),
                    version,
                    mode);
        }
        return packageResolutionRequest;
    }
}
