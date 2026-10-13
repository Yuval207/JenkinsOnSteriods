"use client";

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, FolderGit2, AlertTriangle, ExternalLink, Activity, GitBranch, ShieldAlert, X, Loader2, Sparkles, Folder } from 'lucide-react';
import Link from 'next/link';
import { formatDistanceToNow } from 'date-fns';
import { useState } from 'react';
import axios from 'axios';

type Project = {
  id: string;
  name: string;
  repositoryUrl: string;
  defaultBranch: string;
  status: string;
  detectedLanguages: string;
  detectedBuildTools: string;
  createdAt: string;
  updatedAt: string;
};

export default function ProjectsPage() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newProject, setNewProject] = useState({ name: '', repositoryUrl: '', defaultBranch: 'main' });
  const [addError, setAddError] = useState('');

  const { data: projectsData, isLoading, isError } = useQuery({
    queryKey: ['projects'],
    queryFn: async () => {
      const res = await axios.get('/api/v1/projects');
      return res.data.content as Project[];
    }
  });

  const addProjectMutation = useMutation({
    mutationFn: async (project: typeof newProject) => {
      const res = await axios.post('/api/v1/projects', project);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      setIsModalOpen(false);
      setNewProject({ name: '', repositoryUrl: '', defaultBranch: 'main' });
      setAddError('');
    },
    onError: (error: any) => {
      setAddError(error.response?.data?.message || 'Failed to add project');
    }
  });

  const handleAddDemoApp = () => {
    addProjectMutation.mutate({
      name: 'Demo Vulnerable App',
      repositoryUrl: '/Users/yuvalsharma/Desktop/jenkinsOnSteriods/demo-vulnerable-app',
      defaultBranch: 'main'
    });
  };

  const projects = projectsData || [];

  return (
    <div className="space-y-6 pb-12">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-display text-white">Repositories</h1>
          <p className="text-zinc-400 mt-1 text-sm">Manage source code repositories and trigger automated scanner pipelines.</p>
        </div>
        <div className="flex items-center gap-2">
          <button 
            className="px-3.5 py-2 text-xs font-semibold rounded-lg bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 flex items-center gap-1.5 transition-all shadow-sm"
            onClick={handleAddDemoApp}
            disabled={addProjectMutation.isPending}
          >
            <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
            Add Demo Vulnerable App
          </button>
          <button className="btn-primary" onClick={() => setIsModalOpen(true)}>
            <Plus className="w-4 h-4" />
            Connect Repository
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center items-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-zinc-500" />
        </div>
      ) : isError ? (
        <div className="text-red-400 p-4 rounded bg-red-500/10 border border-red-500/20">
          Failed to load projects. Make sure the backend is running.
        </div>
      ) : projects.length === 0 ? (
        <div className="flex flex-col items-center justify-center p-12 border border-dashed border-white/10 rounded-xl bg-zinc-900/50">
          <FolderGit2 className="w-12 h-12 text-zinc-600 mb-4" />
          <h3 className="text-lg font-medium text-white mb-2">No repositories connected</h3>
          <p className="text-zinc-400 text-center max-w-md mb-6 text-sm">
            Connect a GitHub repository or use the built-in local vulnerable app to test Semgrep, Gitleaks, and Trivy scanners immediately.
          </p>
          <div className="flex items-center gap-3">
            <button 
              className="px-4 py-2 text-sm font-semibold rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white flex items-center gap-2 transition-all shadow-lg shadow-indigo-600/20"
              onClick={handleAddDemoApp}
              disabled={addProjectMutation.isPending}
            >
              <Sparkles className="w-4 h-4" />
              Load Demo Vulnerable App
            </button>
            <button className="btn-secondary" onClick={() => setIsModalOpen(true)}>
              <Plus className="w-4 h-4" />
              Connect Custom Repo
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 stagger-children mt-6">
          {projects.map((project) => (
            <div key={project.id} className="sg-card-interactive flex flex-col p-5 group relative">
              <Link href={`/projects/${project.id}`} className="absolute inset-0 z-10" />
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-lg bg-zinc-800 border border-white/10 flex items-center justify-center shrink-0">
                    <GitBranch className="w-5 h-5 text-zinc-300" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-white text-base leading-tight group-hover:text-indigo-400 transition-colors">
                      {project.name}
                    </h3>
                    <div className="text-xs text-zinc-500 font-mono mt-1 flex items-center gap-1.5 truncate max-w-[200px]">
                      {project.repositoryUrl}
                    </div>
                  </div>
                </div>
              </div>
              
              <div className="flex flex-wrap items-center gap-1.5 mb-6 text-[10px] font-mono text-zinc-400">
                {project.detectedLanguages && project.detectedLanguages.split(',').map(lang => (
                  <span key={lang} className="px-1.5 py-0.5 rounded bg-white/5 border border-white/5">{lang.trim()}</span>
                ))}
                {project.detectedBuildTools && project.detectedBuildTools.split(',').map(tool => (
                  <span key={tool} className="px-1.5 py-0.5 rounded bg-indigo-500/10 text-indigo-300 border border-indigo-500/20">{tool.trim()}</span>
                ))}
              </div>
              
              <div className="mt-auto pt-4 border-t border-white/5 grid grid-cols-2 gap-4">
                <div>
                  <span className="text-[10px] text-zinc-500 uppercase tracking-wider block mb-1">Status</span>
                  <div className="flex items-center gap-2">
                    <span className={`text-xs font-semibold ${project.status === 'ACTIVE' ? 'text-emerald-400' : 'text-red-400'}`}>
                      {project.status}
                    </span>
                  </div>
                </div>
                
                <div>
                  <span className="text-[10px] text-zinc-500 uppercase tracking-wider block mb-1">Added</span>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-medium text-zinc-300">
                      {project.createdAt ? formatDistanceToNow(new Date(project.createdAt), { addSuffix: true }) : 'N/A'}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Project Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
          <div className="bg-zinc-900 border border-white/10 rounded-xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between p-4 border-b border-white/10">
              <h2 className="text-lg font-semibold text-white">Connect Repository</h2>
              <button 
                onClick={() => setIsModalOpen(false)}
                className="text-zinc-400 hover:text-white transition-colors p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <form 
              onSubmit={(e) => {
                e.preventDefault();
                addProjectMutation.mutate(newProject);
              }}
              className="p-5 space-y-4"
            >
              {addError && (
                <div className="p-3 text-sm text-red-400 bg-red-500/10 border border-red-500/20 rounded-lg flex items-start gap-2">
                  <AlertTriangle className="w-4 h-4 mt-0.5 shrink-0" />
                  <span>{addError}</span>
                </div>
              )}

              {/* Demo Pre-fill */}
              <div className="p-3 bg-indigo-500/10 border border-indigo-500/20 rounded-lg flex items-center justify-between">
                <div className="text-xs text-indigo-300">
                  <div className="font-semibold">Local Demo Vulnerable App</div>
                  <div className="text-zinc-400 text-[11px]">Pre-seeded with Java, Docker, and secrets</div>
                </div>
                <button
                  type="button"
                  onClick={() => setNewProject({
                    name: 'Demo Vulnerable App',
                    repositoryUrl: '/Users/yuvalsharma/Desktop/jenkinsOnSteriods/demo-vulnerable-app',
                    defaultBranch: 'main'
                  })}
                  className="px-2.5 py-1 text-xs font-medium bg-indigo-600 hover:bg-indigo-500 text-white rounded transition-colors"
                >
                  Fill In
                </button>
              </div>
              
              <div>
                <label className="block text-sm font-medium text-zinc-300 mb-1.5">Project Name</label>
                <input 
                  type="text" 
                  required
                  placeholder="e.g. My Secure App"
                  className="w-full bg-black/50 border border-white/10 rounded-lg px-3 py-2 text-white placeholder:text-zinc-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500"
                  value={newProject.name}
                  onChange={e => setNewProject({...newProject, name: e.target.value})}
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-zinc-300 mb-1.5">Repository URL or Local Path</label>
                <input 
                  type="text" 
                  required
                  placeholder="https://github.com/org/repo.git or /path/to/local/repo"
                  className="w-full bg-black/50 border border-white/10 rounded-lg px-3 py-2 text-white placeholder:text-zinc-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 font-mono text-xs"
                  value={newProject.repositoryUrl}
                  onChange={e => setNewProject({...newProject, repositoryUrl: e.target.value})}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-zinc-300 mb-1.5">Default Branch</label>
                <input 
                  type="text" 
                  required
                  className="w-full bg-black/50 border border-white/10 rounded-lg px-3 py-2 text-white placeholder:text-zinc-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500"
                  value={newProject.defaultBranch}
                  onChange={e => setNewProject({...newProject, defaultBranch: e.target.value})}
                />
              </div>
              
              <div className="pt-4 flex items-center justify-end gap-3">
                <button 
                  type="button" 
                  className="px-4 py-2 text-sm font-medium text-zinc-300 hover:text-white transition-colors"
                  onClick={() => setIsModalOpen(false)}
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  disabled={addProjectMutation.isPending}
                  className="btn-primary"
                >
                  {addProjectMutation.isPending ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Connecting...
                    </>
                  ) : 'Connect Repository'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
